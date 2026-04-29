/* SPDX-License-Identifier: GPL-2.0 WITH Classpath-exception-2.0 */
/* SPDX-FileCopyrightText: 2023 Damien Goutte-Gattat https://incenp.org/notes/2023/universal-java-app-on-macos.html */
/* SPDX-FileCopyrightText: 2025 Autores de Firmador https://firmador.libre.cr/ */

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <err.h>
#include <dlfcn.h>
#include <pthread.h>
#include <pwd.h>
#include <sys/types.h>

#include <mach-o/dyld.h>
#include <CoreFoundation/CoreFoundation.h>

#include <jni.h>

typedef jint (JNICALL CreateJavaVM_t)(JavaVM **, void **, void *);

static char app_dir[PATH_MAX];

/* Dummy callback for the main thread loop. */
static void
dummy_callback(void *info)
{
    (void) info;
}

static char *
get_application_directory(char *buffer, uint32_t len)
{
    char *last_slash = NULL;
    int n = 2;

    if ( ! _NSGetExecutablePath(buffer, &len) ) {
        while ( n-- > 0 ) {
            if ( (last_slash = strrchr(buffer, '/')) )
                *last_slash = '\0';
        }
    }

    return last_slash ? buffer : NULL;
}

/* Execute the main method of our application. */
static int
start_java_main(JNIEnv *env)
{
    jclass main_class;
    jmethodID main_method;
    jobjectArray main_args;

    if ( ! (main_class = (*env)->FindClass(env, "Firmador")) )
        return -1;

    if ( ! (main_method = (*env)->GetStaticMethodID(env, main_class, "main",
                                                    "([Ljava/lang/String;)V")) )
        return -1;

    main_args = (*env)->NewObjectArray(env, 0,
                                       (*env)->FindClass(env, "java/lang/String"),
                                       (*env)->NewStringUTF(env, ""));

    (*env)->CallStaticVoidMethod(env, main_class, main_method, main_args);

    return 0;
}

/* Load and start the Java virtual machine. */
static void *
start_jvm(void *arg)
{
    char lib_path[PATH_MAX];
    void *lib;
    JavaVMInitArgs jvm_args;
    JavaVMOption jvm_opts[4];
    JavaVM *jvm;
    JNIEnv *env;
    CreateJavaVM_t *create_java_vm;

    (void) arg;

    /* Load the Java library in the bundled JRE. */
    snprintf(lib_path, PATH_MAX, "%s/Resources/jre/lib/libjli.dylib", app_dir);
    if ( ! (lib = dlopen(lib_path, RTLD_LAZY)) )
        errx(EXIT_FAILURE, "Cannot load Java library: %s", dlerror());

    if ( ! (create_java_vm = (CreateJavaVM_t *)dlsym(lib, "JNI_CreateJavaVM")) )
        errx(EXIT_FAILURE, "Cannot find JNI_CreateJavaVM: %s", dlerror());

    /* Determine which JAR to load: external update or bundled fallback. */
    static char classpath_opt[PATH_MAX + 32];
    char external_jar[PATH_MAX];
    struct passwd *pw = getpwuid(getuid());
    if (pw && snprintf(external_jar, PATH_MAX, "%s/Library/Application Support/Firmador/firmador.jar", pw->pw_dir) < PATH_MAX
           && access(external_jar, R_OK) == 0) {
        snprintf(classpath_opt, sizeof(classpath_opt), "-Djava.class.path=%s", external_jar);
    } else {
        snprintf(classpath_opt, sizeof(classpath_opt), "-Djava.class.path=Resources/firmador.jar");
    }

    /* Prepare options for the JVM. */
    jvm_opts[0].optionString = "-Djdk.lang.Process.launchMechanism=fork";
    jvm_opts[1].optionString = "-Dapple.awt.application.appearance=system";
    jvm_opts[2].optionString = classpath_opt;
    jvm_opts[3].optionString = "--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED";
    jvm_args.version = JNI_VERSION_1_2;
    jvm_args.ignoreUnrecognized = JNI_TRUE;
    jvm_args.options = jvm_opts;
    jvm_args.nOptions = 4;

    if ( create_java_vm(&jvm, (void **)&env, &jvm_args) == JNI_ERR )
        errx(EXIT_FAILURE, "Cannot create Java virtual machine");

    if ( start_java_main(env) != 0 ) {
        (*jvm)->DestroyJavaVM(jvm);
        errx(EXIT_FAILURE, "Cannot start Java main method");
    }

    if ( (*env)->ExceptionCheck(env) ) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    (*jvm)->DetachCurrentThread(jvm);
    (*jvm)->DestroyJavaVM(jvm);

    /* Calling exit() here will terminate both this JVM thread and the
     * infinite loop in the main thread. */
    exit(EXIT_SUCCESS);
}

int
main(int argc, char **argv)
{
    pthread_t jvm_thread;
    pthread_attr_t jvm_thread_attr;
    CFRunLoopSourceContext loop_context;
    CFRunLoopSourceRef loop_ref;

    (void) argc;
    (void) argv;

    if ( ! get_application_directory(app_dir, PATH_MAX) )
        errx(EXIT_FAILURE, "Cannot get application directory");

    if ( chdir(app_dir) == -1 )
        err(EXIT_FAILURE, "Cannot change current directory");

    /* Start the thread where the JVM will run. */
    pthread_attr_init(&jvm_thread_attr);
    pthread_attr_setscope(&jvm_thread_attr, PTHREAD_SCOPE_SYSTEM);
    pthread_attr_setdetachstate(&jvm_thread_attr, PTHREAD_CREATE_DETACHED);
    if ( pthread_create(&jvm_thread, &jvm_thread_attr, start_jvm, NULL) != 0 )
        err(EXIT_FAILURE, "Cannot start JVM thread");
    pthread_attr_destroy(&jvm_thread_attr);

    /* Run a dummy loop in the main thread. */
    memset(&loop_context, 0, sizeof(loop_context));
    loop_context.perform = &dummy_callback;
    loop_ref = CFRunLoopSourceCreate(NULL, 0, &loop_context);
    CFRunLoopAddSource(CFRunLoopGetCurrent(), loop_ref, kCFRunLoopCommonModes);
    CFRunLoopRun();

    return EXIT_SUCCESS;
}
