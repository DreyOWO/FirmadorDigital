.. _test_006:

Test 006
===========================================


**Fecha de redacción:** 09/04/2025

**Fecha de ejecución:** 23/04/2025

**Realizado por:** Victor Andrey Jimenez Sanchez

Descripción del Test
--------------------

En la ventana de documentos:
Se debe cargar en la tabla un archivo para cada uno de los tipos de archivo soportados por la aplicación.
Se debe debe previsualizar cada archivo archivo.
En la ventana de Firmar:
se debe firmar el archivo previsualizado.
En caso de poseer una previsualización de la firma, se debe firmar con y sin firma visible.

**Tipos de archivo soportados:**

- **Documentos:** PDF, DOCX, PPTX, XLSX, ODT, ODP, ODS, ODG
- **XML:** XML, XMLA
- **Otros formatos de texto:** TXT, CSV, MD, JSON, YAML
- **Imágenes:** PNG, JPG, BMP, TIFF, SVG
- **Multimedia:** MP3, WAV, MP4
- **Comprimidos:** ZIP, RAR, ASCIS
- **Otros:** QDS

Resultado esperado
------------------

Cada archivo debe ser firmado correctamente en todos los formatos de firma compatibles con su tipo MIME.
La aplicación debe mostrar una confirmación de firma válida para cada caso, y los archivos resultantes deben contener una firma digital funcional y verificable.

Tablero de resultados
---------------------

+-----------+-----------------+
| Archivo   | Se Firmo        |
+-----------+-----------------+
| PDF       | ☑               |
+-----------+-----------------+
| DOCX      | ☑               |
+-----------+-----------------+
| PPTX      | ☑               |
+-----------+-----------------+
| XLSX      | ☑               |
+-----------+-----------------+
| ODT       | ☑               |
+-----------+-----------------+
| ODP       | ☑               |
+-----------+-----------------+
| ODS       | ☑               |
+-----------+-----------------+
| ODG       | ☑               |
+-----------+-----------------+
| XML       | ☑               |
+-----------+-----------------+
| XMLA      | ☑               |
+-----------+-----------------+
| TXT       | ☑               |
+-----------+-----------------+
| CSV       | ☑               |
+-----------+-----------------+
| MD        | ☑               |
+-----------+-----------------+
| JSON      | ☑               |
+-----------+-----------------+
| YAML      | ☑               |
+-----------+-----------------+
| PNG       | ☑               |
+-----------+-----------------+
| JPG       | ☑               |
+-----------+-----------------+
| BMP       | ☑               |
+-----------+-----------------+
| TIFF      | ☑               |
+-----------+-----------------+
| SVG       | ☑               |
+-----------+-----------------+
| MP3       | ☑               |
+-----------+-----------------+
| WAV       | ☑               |
+-----------+-----------------+
| MP4       | ☑               |
+-----------+-----------------+
| ZIP       | ☑               |
+-----------+-----------------+
| RAR       | ☑               |
+-----------+-----------------+
| ASCIS     | ☑               |
+-----------+-----------------+

Observaciones del test
----------------------

*(por completar)*
