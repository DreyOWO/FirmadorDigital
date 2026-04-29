/* Firmador is a program to sign documents using AdES standards.

Copyright (C) Firmador authors.

This file is part of Firmador.

Firmador is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Firmador is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Firmador.  If not, see <http://www.gnu.org/licenses/>.  */

package cr.libre.firmador.ooxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.openxml4j.opc.OPCPackage;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.MimeType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.Digest;

@SuppressWarnings("serial")
public class DSSDocumentOXML implements DSSDocument   {
    private OPCPackage opcpkg;
    protected MimeType mimeType;
    protected String name;
    private String fileName;

    public OPCPackage getOpcpkg() {
        return opcpkg;
    }
    public void setOpcpkg(OPCPackage opcpkg) {
        this.opcpkg = opcpkg;
    }
    public MimeType getMimeType() {
        return mimeType;
    }
    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public DSSDocumentOXML(OPCPackage opcpkg, String fileName) {
        this.opcpkg=opcpkg;
        this.fileName = fileName;
    }
    @Override
    public InputStream openStream() {
       /**
        try {
            File tempFile = File.createTempFile("prefix-", "-suffix");
            tempFile.deleteOnExit();
            return new FileInputStream(tempFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }**/
       try {
           return new FileInputStream(new File(this.fileName));
       } catch (FileNotFoundException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       } // this.opcpkg.;
       return null;
    }

    @Override
    public void writeTo(OutputStream stream) throws IOException {
        this.opcpkg.save(stream);
    }


    @Override
    public void save(String filePath) throws IOException {
        File archive = new File(filePath);
        this.opcpkg.save(archive);

    }


	@Override
	public byte[] getDigestValue(DigestAlgorithm digestAlgorithm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Digest getDigest(DigestAlgorithm digestAlgorithm) {
		// TODO Auto-generated method stub
		return null;
	}

}
