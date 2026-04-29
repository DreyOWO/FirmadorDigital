.. _test_004:

Test 004
===========================================


**Fecha de redacción:** 09/04/2025

**Fecha de ejecución:** 23/04/2025

**Realizado por:** Víctor Andrey Jimenez Sanchez

Descripción del Test
--------------------

En la ventana de documentos:
Se debe cargar en la tabla un archivo para cada uno de los tipos de archivo soportados por la aplicación.

A continuación, se debe presionar el botón Firmar todos para cada documento individualmente.

Para cada archivo, se deben aplicar todas las firmas digitales compatibles según su tipo MIME (por ejemplo: PAdES para PDF, CAdES para Office, XAdES para XML, etc.).

Después de cada firma, el archivo debe ser eliminado y vuelto a cargar en su estado original (sin firmas previas) antes de aplicar el siguiente tipo de firma.
Esto asegura que cada tipo de firma se prueba sobre un archivo limpio.


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

+-----------+-------+-------+-------+------------+---------------+--------+
| Archivo   | PAdES | CAdES | XAdES | OPEN XML   | OPEN DOCUMENT | ASIC-E |
+-----------+-------+-------+-------+------------+---------------+--------+
| PDF       | ☑     | ☐     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| DOCX      | ☐     | ☐     | ☐     | ☑          | ☐             | ☐      |
+-----------+-------+-------+-------+------------+---------------+--------+
| PPTX      | ☐     | ☐     | ☐     | ☑          | ☐             | ☐      |
+-----------+-------+-------+-------+------------+---------------+--------+
| XLSX      | ☐     | ☐     | ☐     | ☑          | ☐             | ☐      |
+-----------+-------+-------+-------+------------+---------------+--------+
| ODT       | ☐     | ☐     | ☐     | ☐          | ☑             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| ODP       | ☐     | ☐     | ☐     | ☐          | ☑             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| ODS       | ☐     | ☐     | ☐     | ☐          | ☑             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| ODG       | ☐     | ☐     | ☐     | ☐          | ☑             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| XML       | ☐     | ☐     | ☑     | ☐          | ☐             | ☒      |
+-----------+-------+-------+-------+------------+---------------+--------+
| XMLA      | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| TXT       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| CSV       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| MD        | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| JSON      | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| YAML      | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| PNG       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| JPG       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| BMP       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| TIFF      | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| SVG       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| MP3       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| WAV       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| MP4       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| ZIP       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| RAR       | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+
| ASCIS     | ☐     | ☑     | ☐     | ☐          | ☐             | ☑      |
+-----------+-------+-------+-------+------------+---------------+--------+

Observaciones del test
----------------------

*(por completar)*
