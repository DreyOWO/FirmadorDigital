pipeline {
  agent none
  stages {
    stage('Build') {
      agent any
      steps {
        sh 'xwfb-run -c cage -- mvn -Dstyle.color=always clean package'
        stash name: 'jar', includes: 'target/firmador.jar'
      }
    }
    stage('Prepackage') {
      agent {
        label 'mac'
      }
      steps {
        unstash 'jar'
        sh '# /opt/homebrew/bin/advzip -z4 target/firmador.jar'
        sh 'sha256sum --quiet target/firmador.jar > target/firmador.jar.sha256'
        archiveArtifacts artifacts: 'target/firmador.jar, target/firmador.jar.sha256', onlyIfSuccessful: true
        stash name: 'smallerjar', includes: 'target/firmador.jar'
      }
    }
    stage('Package') {
      parallel {
        stage('Windows packaging') {
          agent any
          steps {
            sh 'rm -f "build/windows/Instalar Firmador.exe"'
            sh 'ln -sf ../../target/firmador.jar build/windows/'
            sh 'ln -sf ../../../jre build/windows/'
            sh 'makensis "build/windows/Instalar Firmador.nsi"'
            sh 'sha256sum "build/windows/Instalar Firmador.exe" | cut -d " " -f1 > "build/windows/Instalar Firmador.exe.sha256"'
            archiveArtifacts artifacts: 'build/windows/Instalar Firmador.exe, build/windows/Instalar Firmador.exe.sha256', onlyIfSuccessful: true
          }
        }
        stage('Mac packaging') {
          agent {
            label 'mac'
          }
          steps {
            unstash 'smallerjar'
            sh 'rm -f build/macos/Firmador.app/Contents/Resources/firmador.jar'
            sh 'rm -rf build/macos/Firmador.app/Contents/Resources/firmador/'
            sh 'rm -rf build/macos/Firmador.app/Contents/Resources/jre/'
            sh 'cp target/firmador.jar build/macos/Firmador.app/Contents/Resources/'
            sh 'cp -r ../../jre build/macos/Firmador.app/Contents/Resources/'
            dir('build/macos/Firmador.app/Contents/Resources') {
              sh 'ditto -x -k firmador.jar firmador/'
            }
            dir('build/macos') {
              sh '../../../../sign Firmador.app/Contents/Resources/firmador/com/sun/jna/darwin-aarch64/libjnidispatch.jnilib'
              sh '../../../../sign Firmador.app/Contents/Resources/firmador/com/sun/jna/darwin-x86-64/libjnidispatch.jnilib'
            }
            dir('build/macos/Firmador.app/Contents/Resources') {
              sh 'ditto -c --sequesterRsrc -k --zlibCompressionLevel 9 firmador/ firmador.jar'
            }
            dir('build/macos') {
              sh 'cp -a Firmador.app/Contents/Resources/firmador.jar .'
              sh '../../../../sign Firmador.zip Firmador.app'
            }
            sh '# /opt/homebrew/bin/advzip -z4 build/macos/Firmador.zip'
            sh 'sha256sum --quiet build/macos/firmador.jar > build/macos/firmador.jar.sha256'
            sh 'sha256sum --quiet build/macos/Firmador.zip > build/macos/Firmador.zip.sha256'
            archiveArtifacts artifacts: 'build/macos/firmador.jar, build/macos/firmador.jar.sha256, build/macos/Firmador.zip, build/macos/Firmador.zip.sha256', onlyIfSuccessful: true
          }
        }
      }
    }
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '1'))
  }
}
