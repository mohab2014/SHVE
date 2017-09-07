package edu.monash.shangqi.hve.core.impl;

import edu.monash.shangqi.hve.core.PredicateOnlyAESSymmetricBlockCipher;
import edu.monash.shangqi.hve.param.SHVEKeyParameter;
import edu.monash.shangqi.hve.param.impl.SHVEEncryptionParameter;
import edu.monash.shangqi.hve.param.impl.SHVEMasterSecretKeyParameter;
import edu.monash.shangqi.hve.param.impl.SHVESecretKeyParameter;
import edu.monash.shangqi.hve.util.AESUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class SHVEPredicateEngine
        extends PredicateOnlyAESSymmetricBlockCipher {

    private int size;

    public SHVEPredicateEngine() {
    }

    public void initialize() {
        if (this.forEncryption) {
            if (!(this.key instanceof SHVEEncryptionParameter)) {
                throw new IllegalArgumentException("SHVEEncryptionParameter are required for encryption.");
            }
        } else if(!(this.key instanceof SHVESecretKeyParameter)) {
            throw new IllegalArgumentException("SHVESecretKeyParameter are required for decryption.");
        }

        SHVEKeyParameter hveKey = (SHVEKeyParameter) this.key;
        this.size = hveKey.getParameter().getSize();
    }

    public byte[] process(byte[] in, int inOff, int inLen) {
        ArrayList<String> C;
        ArrayList<String> S;

        if (this.key instanceof SHVESecretKeyParameter) {   // evaluation
            int offset = inOff;
            SHVESecretKeyParameter secretKey = (SHVESecretKeyParameter)this.key;
            C = new ArrayList<>();
            S = new ArrayList<>();

            DataInputStream inputStream;
            inputStream = new DataInputStream(new ByteArrayInputStream(in));

            for(int i = 0; i < this.size; ++i) {
                try {
                    byte[] res = new byte[24];
                    inputStream.read(res);
                    C.add(new String(res));
                    inputStream.read(res);
                    S.add(new String(res));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

            boolean result = true;
            for(int i = 0; i < secretKey.getParameter().getSize(); ++i) {
                if (secretKey.isStar(i)) {
                    result &= secretKey.getDAt(i).equals(S.get(i));
                } else {
                    result &= secretKey.getDAt(i).equals(C.get(i));
                }
            }

            return new byte[]{(byte)(result ? 1 : 0)};

        } else if (inLen <= this.inBytes && inLen >= this.inBytes) {    // encryption
            SHVEEncryptionParameter encParams = (SHVEEncryptionParameter)this.key;
            SHVEMasterSecretKeyParameter pk = encParams.getMasterSecretKey();
            C = new ArrayList<>();
            S = new ArrayList<>();

            for(int i = 0; i < this.size; ++i) {
                int j = encParams.getAttributeAt(i);
                C.add(AESUtil.encrypt(String.valueOf(j).concat(String.valueOf(i)), pk.getMSK()));
                S.add(AESUtil.encrypt(String.valueOf(-1).concat(String.valueOf(i)), pk.getMSK()));
            }

            ByteArrayOutputStream outputStream;
            try {
                outputStream = new ByteArrayOutputStream(this.getOutputBlockSize());
                for (int i = 0; i < this.size; ++i) {
                    outputStream.write(C.get(i).getBytes());
                    outputStream.write(S.get(i).getBytes());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return outputStream.toByteArray();
        }
        return null;
    }
}