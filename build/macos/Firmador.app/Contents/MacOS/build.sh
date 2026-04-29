clang \
  -arch x86_64 \
  -arch arm64 \
  -mmacos-version-min=11.0 \
  -ansi -Wall -Wextra -Wpedantic \
  -I$HOME/integracion.libre.cr/jdk/include \
  -I$HOME/integracion.libre.cr/jdk/include/darwin \
  -Oz -Wl,-S -Wl,-x \
  -framework Cocoa \
  -o firmador firmador.c
