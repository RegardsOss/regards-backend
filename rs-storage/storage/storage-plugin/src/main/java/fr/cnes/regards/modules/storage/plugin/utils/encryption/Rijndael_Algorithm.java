package fr.cnes.regards.modules.storage.plugin.utils.encryption;

import java.io.PrintWriter;
import java.security.InvalidKeyException;

/**
 * Cette classe contient l'algorithmie permettant l'implementation de code AES Le commentaire qui suit est l'original.
 *
 * Rijndael --pronounced Reindaal-- is a variable block-size (128-, 192- and 256-bit), variable key-size (128-, 192- and
 * 256-bit) symmetric cipher.
 * <p>
 *
 * Rijndael was written by <a href="mailto:rijmen@esat.kuleuven.ac.be">Vincent Rijmen</a> and <a
 * href="mailto:Joan.Daemen@village.uunet.be">Joan Daemen</a>. <a href="http://www.cryptix.org" />
 *
 * Cryptix General License
 *
 * Copyright (c) 1995-2005 The Cryptix Foundation Limited. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the copyright notice, this list of conditions and the following
 * disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE CRYPTIX FOUNDATION LIMITED AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE CRYPTIX FOUNDATION LIMITED OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * <b>$Revision: 1.4 $</b>
 *
 * @author Raif S. Naffah
 * @author Paulo S. L. M. Barreto
 */
public final class Rijndael_Algorithm // implicit no-argument constructor
{

    // Debugging methods and variables
    // ...........................................................................

    /**
     * Nom
     */
    static final String NAME = "Rijndael_Algorithm";

    /**
     * Var
     */
    static final boolean IN = true;

    /**
     * Var
     */
    static final boolean OUT = false;

    /**
     * Var
     */
    static final boolean DEBUG = Rijndael_Properties.GLOBAL_DEBUG;

    /**
     * Debug
     */
    static final int debuglevel = DEBUG ? Rijndael_Properties.getLevel(NAME) : 0;

    /**
     * Canal erreur
     */
    static final PrintWriter err = DEBUG ? Rijndael_Properties.getOutput() : null;

    /**
     * Trace ?
     */
    static final boolean TRACE = Rijndael_Properties.isTraceable(NAME);

    /**
     * Debug
     *
     * @param s
     *            la chaine à afficher
     */
    static void debug(String s) {
        err.println(">>> " + NAME + ": " + s);
    }

    /**
     * Trace
     *
     * @param in
     *            booleen
     * @param s
     *            la chaine a afficher
     */
    static void trace(boolean in, String s) {
        if (TRACE) {
            err.println((in ? "==> " : "<== ") + NAME + "." + s);
        }
    }

    /**
     * Trace
     *
     * @param s
     *            la chaine a afficher
     */
    static void trace(String s) {
        if (TRACE) {
            err.println("<=> " + NAME + "." + s);
        }
    }

    // Constants and variables
    // ...........................................................................
    /**
     * Taille d'un block
     */
    static final int BLOCK_SIZE = 16; // default block size in bytes

    /**
     * Cte
     */
    static final int[] alog = new int[256];

    /**
     * Cte
     */
    static final int[] log = new int[256];

    /**
     * Cte
     */
    static final byte[] S = new byte[256];

    /**
     * Cte
     */
    static final byte[] Si = new byte[256];

    /**
     * Cte
     */
    static final int[] T1 = new int[256];

    /**
     * Cte
     */
    static final int[] T2 = new int[256];

    /**
     * Cte
     */
    static final int[] T3 = new int[256];

    /**
     * Cte
     */
    static final int[] T4 = new int[256];

    /**
     * Cte
     */
    static final int[] T5 = new int[256];

    /**
     * Cte
     */
    static final int[] T6 = new int[256];

    /**
     * Cte
     */
    static final int[] T7 = new int[256];

    /**
     * Cte
     */
    static final int[] T8 = new int[256];

    /**
     * Cte
     */
    static final int[] U1 = new int[256];

    /**
     * Cte
     */
    static final int[] U2 = new int[256];

    /**
     * Cte
     */
    static final int[] U3 = new int[256];

    /**
     * Cte
     */
    static final int[] U4 = new int[256];

    /**
     * Cte
     */
    static final byte[] rcon = new byte[30];

    /**
     * Cte
     */
    static final int[][][] shifts = new int[][][] { { { 0, 0 }, { 1, 3 }, { 2, 2 }, { 3, 1 } },
            { { 0, 0 }, { 1, 5 }, { 2, 4 }, { 3, 3 } }, { { 0, 0 }, { 1, 7 }, { 3, 5 }, { 4, 4 } } };

    /**
     * Cte
     */
    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F' };

    // Static code - to intialise S-boxes and T-boxes
    // ...........................................................................

    static {
        long time = System.currentTimeMillis();

        int ROOT = 0x11B;
        int i, j = 0;

        //
        // produce log and alog tables, needed for multiplying in the
        // field GF(2^m) (generator = 3)
        //
        alog[0] = 1;
        for (i = 1; i < 256; i++) {
            j = (alog[i - 1] << 1) ^ alog[i - 1];
            if ((j & 0x100) != 0) {
                j ^= ROOT;
            }
            alog[i] = j;
        }
        for (i = 1; i < 255; i++) {
            log[alog[i]] = i;
        }
        byte[][] A = new byte[][] { { 1, 1, 1, 1, 1, 0, 0, 0 }, { 0, 1, 1, 1, 1, 1, 0, 0 }, { 0, 0, 1, 1, 1, 1, 1, 0 },
                { 0, 0, 0, 1, 1, 1, 1, 1 }, { 1, 0, 0, 0, 1, 1, 1, 1 }, { 1, 1, 0, 0, 0, 1, 1, 1 },
                { 1, 1, 1, 0, 0, 0, 1, 1 }, { 1, 1, 1, 1, 0, 0, 0, 1 } };
        byte[] B = new byte[] { 0, 1, 1, 0, 0, 0, 1, 1 };

        //
        // substitution box based on F^{-1}(x)
        //
        int t;
        byte[][] box = new byte[256][8];
        box[1][7] = 1;
        for (i = 2; i < 256; i++) {
            j = alog[255 - log[i]];
            for (t = 0; t < 8; t++) {
                box[i][t] = (byte) ((j >>> (7 - t)) & 0x01);
            }
        }
        //
        // affine transform: box[i] <- B + A*box[i]
        //
        byte[][] cox = new byte[256][8];
        for (i = 0; i < 256; i++) {
            for (t = 0; t < 8; t++) {
                cox[i][t] = B[t];
                for (j = 0; j < 8; j++) {
                    cox[i][t] ^= A[t][j] * box[i][j];
                }
            }
        }
        //
        // S-boxes and inverse S-boxes
        //
        for (i = 0; i < 256; i++) {
            S[i] = (byte) (cox[i][0] << 7);
            for (t = 1; t < 8; t++) {
                S[i] ^= cox[i][t] << (7 - t);
            }
            Si[S[i] & 0xFF] = (byte) i;
        }
        //
        // T-boxes
        //
        byte[][] G = new byte[][] { { 2, 1, 1, 3 }, { 3, 2, 1, 1 }, { 1, 3, 2, 1 }, { 1, 1, 3, 2 } };
        byte[][] AA = new byte[4][8];
        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                AA[i][j] = G[i][j];
            }
            AA[i][i + 4] = 1;
        }
        byte pivot, tmp;
        byte[][] iG = new byte[4][4];
        for (i = 0; i < 4; i++) {
            pivot = AA[i][i];
            if (pivot == 0) {
                t = i + 1;
                while ((AA[t][i] == 0) && (t < 4)) {
                    t++;
                }
                if (t == 4) {
                    throw new RuntimeException("G matrix is not invertible");
                } else {
                    for (j = 0; j < 8; j++) {
                        tmp = AA[i][j];
                        AA[i][j] = AA[t][j];
                        AA[t][j] = tmp;
                    }
                    pivot = AA[i][i];
                }
            }
            for (j = 0; j < 8; j++) {
                if (AA[i][j] != 0) {
                    AA[i][j] = (byte) alog[((255 + log[AA[i][j] & 0xFF]) - log[pivot & 0xFF]) % 255];
                }
            }
            for (t = 0; t < 4; t++) {
                if (i != t) {
                    for (j = i + 1; j < 8; j++) {
                        AA[t][j] ^= mul(AA[i][j], AA[t][i]);
                    }
                    AA[t][i] = 0;
                }
            }
        }
        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                iG[i][j] = AA[i][j + 4];
            }
        }

        int s;
        for (t = 0; t < 256; t++) {
            s = S[t];
            T1[t] = mul4(s, G[0]);
            T2[t] = mul4(s, G[1]);
            T3[t] = mul4(s, G[2]);
            T4[t] = mul4(s, G[3]);

            s = Si[t];
            T5[t] = mul4(s, iG[0]);
            T6[t] = mul4(s, iG[1]);
            T7[t] = mul4(s, iG[2]);
            T8[t] = mul4(s, iG[3]);

            U1[t] = mul4(t, iG[0]);
            U2[t] = mul4(t, iG[1]);
            U3[t] = mul4(t, iG[2]);
            U4[t] = mul4(t, iG[3]);
        }
        //
        // round constants
        //
        rcon[0] = 1;
        int r = 1;
        for (t = 1; t < 30;) {
            rcon[t++] = (byte) (r = mul(2, r));
        }

        time = System.currentTimeMillis() - time;
    }

    // multiply two elements of GF(2^m)
    /**
     * Method
     *
     * @param a
     *            param
     * @param b
     *            param
     *
     * @return return value
     */
    static final int mul(int a, int b) {
        return ((a != 0) && (b != 0)) ? alog[(log[a & 0xFF] + log[b & 0xFF]) % 255] : 0;
    }

    // convenience method used in generating Transposition boxes
    /**
     * Method
     *
     * @param a
     *            param
     * @param b
     *            param
     * @return return value
     */
    static final int mul4(int a, byte[] b) {
        if (a == 0) {
            return 0;
        }
        a = log[a & 0xFF];
        int a0 = (b[0] != 0) ? alog[(a + log[b[0] & 0xFF]) % 255] & 0xFF : 0;
        int a1 = (b[1] != 0) ? alog[(a + log[b[1] & 0xFF]) % 255] & 0xFF : 0;
        int a2 = (b[2] != 0) ? alog[(a + log[b[2] & 0xFF]) % 255] & 0xFF : 0;
        int a3 = (b[3] != 0) ? alog[(a + log[b[3] & 0xFF]) % 255] & 0xFF : 0;
        return (a0 << 24) | (a1 << 16) | (a2 << 8) | a3;
    }

    // Basic API methods
    // ...........................................................................

    /**
     * Convenience method to expand a user-supplied key material into a session key, assuming Rijndael's default block
     * size (128-bit).
     *
     * @param k
     *            The 128/192/256-bit user-key to use.
     * @exception InvalidKeyException
     *                If the key is invalid.
     * @return clé
     */
    public static Object makeKey(byte[] k) throws InvalidKeyException {
        return makeKey(k, BLOCK_SIZE);
    }

    /**
     * Convenience method to encrypt exactly one block of plaintext, assuming Rijndael's default block size (128-bit).
     *
     * @param in
     *            The plaintext.
     * @param inOffset
     *            Index of in from which to start considering data.
     * @param sessionKey
     *            The session key to use for encryption.
     * @return The ciphertext generated from a plaintext using the session key.
     */
    public static byte[] blockEncrypt(byte[] in, int inOffset, Object sessionKey) {
        if (DEBUG) {
            trace(IN, "blockEncrypt(" + in + ", " + inOffset + ", " + sessionKey + ")");
        }
        int[][] Ke = (int[][]) ((Object[]) sessionKey)[0]; // extract encryption round keys
        int ROUNDS = Ke.length - 1;
        int[] Ker = Ke[0];

        // plaintext to ints + key
        int t0 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Ker[0];
        int t1 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Ker[1];
        int t2 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Ker[2];
        int t3 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Ker[3];

        int a0, a1, a2, a3;
        for (int r = 1; r < ROUNDS; r++) { // apply round transforms
            Ker = Ke[r];
            a0 = (T1[(t0 >>> 24) & 0xFF] ^ T2[(t1 >>> 16) & 0xFF] ^ T3[(t2 >>> 8) & 0xFF] ^ T4[t3 & 0xFF]) ^ Ker[0];
            a1 = (T1[(t1 >>> 24) & 0xFF] ^ T2[(t2 >>> 16) & 0xFF] ^ T3[(t3 >>> 8) & 0xFF] ^ T4[t0 & 0xFF]) ^ Ker[1];
            a2 = (T1[(t2 >>> 24) & 0xFF] ^ T2[(t3 >>> 16) & 0xFF] ^ T3[(t0 >>> 8) & 0xFF] ^ T4[t1 & 0xFF]) ^ Ker[2];
            a3 = (T1[(t3 >>> 24) & 0xFF] ^ T2[(t0 >>> 16) & 0xFF] ^ T3[(t1 >>> 8) & 0xFF] ^ T4[t2 & 0xFF]) ^ Ker[3];
            t0 = a0;
            t1 = a1;
            t2 = a2;
            t3 = a3;
        }

        // last round is special
        byte[] result = new byte[BLOCK_SIZE]; // the resulting ciphertext
        Ker = Ke[ROUNDS];
        int tt = Ker[0];
        result[0] = (byte) (S[(t0 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[1] = (byte) (S[(t1 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[2] = (byte) (S[(t2 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[3] = (byte) (S[t3 & 0xFF] ^ tt);
        tt = Ker[1];
        result[4] = (byte) (S[(t1 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[5] = (byte) (S[(t2 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[6] = (byte) (S[(t3 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[7] = (byte) (S[t0 & 0xFF] ^ tt);
        tt = Ker[2];
        result[8] = (byte) (S[(t2 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[9] = (byte) (S[(t3 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[10] = (byte) (S[(t0 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[11] = (byte) (S[t1 & 0xFF] ^ tt);
        tt = Ker[3];
        result[12] = (byte) (S[(t3 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[13] = (byte) (S[(t0 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[14] = (byte) (S[(t1 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[15] = (byte) (S[t2 & 0xFF] ^ tt);

        if (DEBUG) {
            trace(OUT, "blockEncrypt()");
        }
        return result;
    }

    /**
     * Convenience method to decrypt exactly one block of plaintext, assuming Rijndael's default block size (128-bit).
     *
     * @param in
     *            The ciphertext.
     * @param inOffset
     *            Index of in from which to start considering data.
     * @param sessionKey
     *            The session key to use for decryption.
     * @return The plaintext generated from a ciphertext using the session key.
     */
    public static byte[] blockDecrypt(byte[] in, int inOffset, Object sessionKey) {
        if (DEBUG) {
            trace(IN, "blockDecrypt(" + in + ", " + inOffset + ", " + sessionKey + ")");
        }
        int[][] Kd = (int[][]) ((Object[]) sessionKey)[1]; // extract decryption round keys
        int ROUNDS = Kd.length - 1;
        int[] Kdr = Kd[0];

        // ciphertext to ints + key
        int t0 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Kdr[0];
        int t1 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Kdr[1];
        int t2 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Kdr[2];
        int t3 = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                | (in[inOffset++] & 0xFF)) ^ Kdr[3];

        int a0, a1, a2, a3;
        for (int r = 1; r < ROUNDS; r++) { // apply round transforms
            Kdr = Kd[r];
            a0 = (T5[(t0 >>> 24) & 0xFF] ^ T6[(t3 >>> 16) & 0xFF] ^ T7[(t2 >>> 8) & 0xFF] ^ T8[t1 & 0xFF]) ^ Kdr[0];
            a1 = (T5[(t1 >>> 24) & 0xFF] ^ T6[(t0 >>> 16) & 0xFF] ^ T7[(t3 >>> 8) & 0xFF] ^ T8[t2 & 0xFF]) ^ Kdr[1];
            a2 = (T5[(t2 >>> 24) & 0xFF] ^ T6[(t1 >>> 16) & 0xFF] ^ T7[(t0 >>> 8) & 0xFF] ^ T8[t3 & 0xFF]) ^ Kdr[2];
            a3 = (T5[(t3 >>> 24) & 0xFF] ^ T6[(t2 >>> 16) & 0xFF] ^ T7[(t1 >>> 8) & 0xFF] ^ T8[t0 & 0xFF]) ^ Kdr[3];
            t0 = a0;
            t1 = a1;
            t2 = a2;
            t3 = a3;
        }

        // last round is special
        byte[] result = new byte[16]; // the resulting plaintext
        Kdr = Kd[ROUNDS];
        int tt = Kdr[0];
        result[0] = (byte) (Si[(t0 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[1] = (byte) (Si[(t3 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[2] = (byte) (Si[(t2 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[3] = (byte) (Si[t1 & 0xFF] ^ tt);
        tt = Kdr[1];
        result[4] = (byte) (Si[(t1 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[5] = (byte) (Si[(t0 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[6] = (byte) (Si[(t3 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[7] = (byte) (Si[t2 & 0xFF] ^ tt);
        tt = Kdr[2];
        result[8] = (byte) (Si[(t2 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[9] = (byte) (Si[(t1 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[10] = (byte) (Si[(t0 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[11] = (byte) (Si[t3 & 0xFF] ^ tt);
        tt = Kdr[3];
        result[12] = (byte) (Si[(t3 >>> 24) & 0xFF] ^ (tt >>> 24));
        result[13] = (byte) (Si[(t2 >>> 16) & 0xFF] ^ (tt >>> 16));
        result[14] = (byte) (Si[(t1 >>> 8) & 0xFF] ^ (tt >>> 8));
        result[15] = (byte) (Si[t0 & 0xFF] ^ tt);

        if (DEBUG) {
            trace(OUT, "blockDecrypt()");
        }
        return result;
    }

    /**
     * A basic symmetric encryption/decryption test.
     *
     * @return
     */
    public static boolean self_test() {
        return self_test(BLOCK_SIZE);
    }

    // Rijndael own methods
    // ...........................................................................

    /** @return The default length in bytes of the Algorithm input block. */
    public static int blockSize() {
        return BLOCK_SIZE;
    }

    /**
     * Expand a user-supplied key material into a session key.
     *
     * @param k
     *            The 128/192/256-bit user-key to use.
     * @param blockSize
     *            The block size in bytes of this Rijndael.
     * @exception InvalidKeyException
     *                If the key is invalid.
     * @return key
     */
    public static synchronized Object makeKey(byte[] k, int blockSize) throws InvalidKeyException {
        if (DEBUG) {
            trace(IN, "makeKey(" + k + ", " + blockSize + ")");
        }
        if (k == null) {
            throw new InvalidKeyException("Empty key");
        }
        if (!((k.length == 16) || (k.length == 24) || (k.length == 32))) {
            throw new InvalidKeyException("Incorrect key length");
        }
        int ROUNDS = getRounds(k.length, blockSize);
        int BC = blockSize / 4;
        int[][] Ke = new int[ROUNDS + 1][BC]; // encryption round keys
        int[][] Kd = new int[ROUNDS + 1][BC]; // decryption round keys
        int ROUND_KEY_COUNT = (ROUNDS + 1) * BC;
        int KC = k.length / 4;
        int[] tk = new int[KC];
        int i, j;

        // copy user material bytes into temporary ints
        for (i = 0, j = 0; i < KC;) {
            tk[i++] = ((k[j++] & 0xFF) << 24) | ((k[j++] & 0xFF) << 16) | ((k[j++] & 0xFF) << 8) | (k[j++] & 0xFF);
        }
        // copy values into round key arrays
        int t = 0;
        for (j = 0; (j < KC) && (t < ROUND_KEY_COUNT); j++, t++) {
            Ke[t / BC][t % BC] = tk[j];
            Kd[ROUNDS - (t / BC)][t % BC] = tk[j];
        }
        int tt, rconpointer = 0;
        while (t < ROUND_KEY_COUNT) {
            // extrapolate using phi (the round key evolution function)
            tt = tk[KC - 1];
            tk[0] ^= ((S[(tt >>> 16) & 0xFF] & 0xFF) << 24) ^ ((S[(tt >>> 8) & 0xFF] & 0xFF) << 16)
                    ^ ((S[tt & 0xFF] & 0xFF) << 8) ^ (S[(tt >>> 24) & 0xFF] & 0xFF)
                    ^ ((rcon[rconpointer++] & 0xFF) << 24);
            if (KC != 8) {
                for (i = 1, j = 0; i < KC;) {
                    tk[i++] ^= tk[j++];
                }
            } else {
                for (i = 1, j = 0; i < (KC / 2);) {
                    tk[i++] ^= tk[j++];
                }
                tt = tk[(KC / 2) - 1];
                tk[KC / 2] ^= (S[tt & 0xFF] & 0xFF) ^ ((S[(tt >>> 8) & 0xFF] & 0xFF) << 8)
                        ^ ((S[(tt >>> 16) & 0xFF] & 0xFF) << 16) ^ ((S[(tt >>> 24) & 0xFF] & 0xFF) << 24);
                for (j = KC / 2, i = j + 1; i < KC;) {
                    tk[i++] ^= tk[j++];
                }
            }
            // copy values into round key arrays
            for (j = 0; (j < KC) && (t < ROUND_KEY_COUNT); j++, t++) {
                Ke[t / BC][t % BC] = tk[j];
                Kd[ROUNDS - (t / BC)][t % BC] = tk[j];
            }
        }
        for (int r = 1; r < ROUNDS; r++) {
            for (j = 0; j < BC; j++) {
                tt = Kd[r][j];
                Kd[r][j] = U1[(tt >>> 24) & 0xFF] ^ U2[(tt >>> 16) & 0xFF] ^ U3[(tt >>> 8) & 0xFF] ^ U4[tt & 0xFF];
            }
        }
        // assemble the encryption (Ke) and decryption (Kd) round keys into
        // one sessionKey object
        Object[] sessionKey = new Object[] { Ke, Kd };
        if (DEBUG) {
            trace(OUT, "makeKey()");
        }
        return sessionKey;
    }

    /**
     * Encrypt exactly one block of plaintext.
     *
     * @param in
     *            The plaintext.
     * @param inOffset
     *            Index of in from which to start considering data.
     * @param sessionKey
     *            The session key to use for encryption.
     * @param blockSize
     *            The block size in bytes of this Rijndael.
     * @return The ciphertext generated from a plaintext using the session key.
     */
    public static byte[] blockEncrypt(byte[] in, int inOffset, Object sessionKey, int blockSize) {
        if (blockSize == BLOCK_SIZE) {
            return blockEncrypt(in, inOffset, sessionKey);
        }
        if (DEBUG) {
            trace(IN, "blockEncrypt(" + in + ", " + inOffset + ", " + sessionKey + ", " + blockSize + ")");
        }
        Object[] sKey = (Object[]) sessionKey; // extract encryption round keys
        int[][] Ke = (int[][]) sKey[0];

        int BC = blockSize / 4;
        int ROUNDS = Ke.length - 1;
        int SC = BC == 4 ? 0 : (BC == 6 ? 1 : 2);
        int s1 = shifts[SC][1][0];
        int s2 = shifts[SC][2][0];
        int s3 = shifts[SC][3][0];
        int[] a = new int[BC];
        int[] t = new int[BC]; // temporary work array
        int i;
        byte[] result = new byte[blockSize]; // the resulting ciphertext
        int j = 0, tt;

        for (i = 0; i < BC; i++) {
            t[i] = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                    | (in[inOffset++] & 0xFF)) ^ Ke[0][i];
        }
        for (int r = 1; r < ROUNDS; r++) { // apply round transforms
            for (i = 0; i < BC; i++) {
                a[i] = (T1[(t[i] >>> 24) & 0xFF] ^ T2[(t[(i + s1) % BC] >>> 16) & 0xFF]
                        ^ T3[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ T4[t[(i + s3) % BC] & 0xFF]) ^ Ke[r][i];
            }
            System.arraycopy(a, 0, t, 0, BC);
        }
        for (i = 0; i < BC; i++) { // last round is special
            tt = Ke[ROUNDS][i];
            result[j++] = (byte) (S[(t[i] >>> 24) & 0xFF] ^ (tt >>> 24));
            result[j++] = (byte) (S[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ (tt >>> 16));
            result[j++] = (byte) (S[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ (tt >>> 8));
            result[j++] = (byte) (S[t[(i + s3) % BC] & 0xFF] ^ tt);
        }

        if (DEBUG) {
            trace(OUT, "blockEncrypt()");
        }
        return result;
    }

    /**
     * Decrypt exactly one block of ciphertext.
     *
     * @param in
     *            The ciphertext.
     * @param inOffset
     *            Index of in from which to start considering data.
     * @param sessionKey
     *            The session key to use for decryption.
     * @param blockSize
     *            The block size in bytes of this Rijndael.
     * @return The plaintext generated from a ciphertext using the session key.
     */
    public static byte[] blockDecrypt(byte[] in, int inOffset, Object sessionKey, int blockSize) {
        if (blockSize == BLOCK_SIZE) {
            return blockDecrypt(in, inOffset, sessionKey);
        }
        if (DEBUG) {
            trace(IN, "blockDecrypt(" + in + ", " + inOffset + ", " + sessionKey + ", " + blockSize + ")");
        }
        Object[] sKey = (Object[]) sessionKey; // extract decryption round keys
        int[][] Kd = (int[][]) sKey[1];

        int BC = blockSize / 4;
        int ROUNDS = Kd.length - 1;
        int SC = BC == 4 ? 0 : (BC == 6 ? 1 : 2);
        int s1 = shifts[SC][1][1];
        int s2 = shifts[SC][2][1];
        int s3 = shifts[SC][3][1];
        int[] a = new int[BC];
        int[] t = new int[BC]; // temporary work array
        int i;
        byte[] result = new byte[blockSize]; // the resulting plaintext
        int j = 0, tt;

        for (i = 0; i < BC; i++) {
            t[i] = (((in[inOffset++] & 0xFF) << 24) | ((in[inOffset++] & 0xFF) << 16) | ((in[inOffset++] & 0xFF) << 8)
                    | (in[inOffset++] & 0xFF)) ^ Kd[0][i];
        }
        for (int r = 1; r < ROUNDS; r++) { // apply round transforms
            for (i = 0; i < BC; i++) {
                a[i] = (T5[(t[i] >>> 24) & 0xFF] ^ T6[(t[(i + s1) % BC] >>> 16) & 0xFF]
                        ^ T7[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ T8[t[(i + s3) % BC] & 0xFF]) ^ Kd[r][i];
            }
            System.arraycopy(a, 0, t, 0, BC);
        }
        for (i = 0; i < BC; i++) { // last round is special
            tt = Kd[ROUNDS][i];
            result[j++] = (byte) (Si[(t[i] >>> 24) & 0xFF] ^ (tt >>> 24));
            result[j++] = (byte) (Si[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ (tt >>> 16));
            result[j++] = (byte) (Si[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ (tt >>> 8));
            result[j++] = (byte) (Si[t[(i + s3) % BC] & 0xFF] ^ tt);
        }

        if (DEBUG) {
            trace(OUT, "blockDecrypt()");
        }
        return result;
    }

    /**
     * A basic symmetric encryption/decryption test for a given key size.
     *
     * @param keysize
     * @return
     */
    private static boolean self_test(int keysize) {
        if (DEBUG) {
            trace(IN, "self_test(" + keysize + ")");
        }
        boolean ok = false;
        try {
            byte[] kb = new byte[keysize];
            byte[] pt = new byte[BLOCK_SIZE];
            int i;

            for (i = 0; i < keysize; i++) {
                kb[i] = (byte) i;
            }
            for (i = 0; i < BLOCK_SIZE; i++) {
                pt[i] = (byte) i;
            }

            Object key = makeKey(kb, BLOCK_SIZE);
            byte[] ct = blockEncrypt(pt, 0, key, BLOCK_SIZE);
            byte[] cpt = blockDecrypt(ct, 0, key, BLOCK_SIZE);

            ok = areEqual(pt, cpt);
            if (!ok) {
                throw new RuntimeException("Symmetric operation failed");
            }
        } catch (Exception x) {
        }
        if (DEBUG) {
            trace(OUT, "self_test()");
        }
        return ok;
    }

    /**
     * Return The number of rounds for a given Rijndael's key and block sizes.
     *
     * @param keySize
     *            The size of the user key material in bytes.
     * @param blockSize
     *            The desired block size in bytes.
     * @return The number of rounds for a given Rijndael's key and block sizes.
     */
    public static int getRounds(int keySize, int blockSize) {
        switch (keySize) {
            case 16:
                return blockSize == 16 ? 10 : (blockSize == 24 ? 12 : 14);
            case 24:
                return blockSize != 32 ? 12 : 14;
            default: // 32 bytes = 256 bits
                return 14;
        }
    }

    // utility static methods (from cryptix.util.core ArrayUtil and Hex classes)
    // ...........................................................................

    /**
     * Compares two byte arrays for equality.
     *
     * @return true if the arrays have identical contents
     * @param a
     *            array a
     * @param b
     *            array b
     */
    private static boolean areEqual(byte[] a, byte[] b) {
        int aLength = a.length;
        if (aLength != b.length) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a string of 2 hexadecimal digits (most significant digit first) corresponding to the lowest 8 bits of
     * <i>n</i>.
     *
     * @param n
     * @return
     */
    protected static String byteToString(int n) {
        char[] buf = { HEX_DIGITS[(n >>> 4) & 0x0F], HEX_DIGITS[n & 0x0F] };
        return new String(buf);
    }

    /**
     * Returns a string of 2 hexadecimal digits (most significant digit first) corresponding to the lowest 8 bits of
     * <i>n</i>.
     *
     * @param n
     *            param a transformer
     * @return un byte
     */
    protected static String intToString(int n) {
        char[] buf = new char[8];
        for (int i = 7; i >= 0; i--) {
            buf[i] = HEX_DIGITS[n & 0x0F];
            n >>>= 4;
        }
        return new String(buf);
    }

    /**
     * Returns a string of hexadecimal digits from a byte array. Each byte is converted to 2 hex symbols.
     *
     * @param ba
     * @return
     */
    protected static String toString(byte[] ba) {
        int length = ba.length;
        char[] buf = new char[length * 2];
        for (int i = 0, j = 0, k; i < length;) {
            k = ba[i++];
            buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
            buf[j++] = HEX_DIGITS[k & 0x0F];
        }
        return new String(buf);
    }

    /**
     * Returns a string of hexadecimal digits from an integer array. Each int is converted to 4 hex symbols.
     *
     * @param ia
     * @return
     */
    protected static String toString(int[] ia) {
        int length = ia.length;
        char[] buf = new char[length * 8];
        for (int i = 0, j = 0, k; i < length; i++) {
            k = ia[i];
            buf[j++] = HEX_DIGITS[(k >>> 28) & 0x0F];
            buf[j++] = HEX_DIGITS[(k >>> 24) & 0x0F];
            buf[j++] = HEX_DIGITS[(k >>> 20) & 0x0F];
            buf[j++] = HEX_DIGITS[(k >>> 16) & 0x0F];
            buf[j++] = HEX_DIGITS[(k >>> 12) & 0x0F];
            buf[j++] = HEX_DIGITS[(k >>> 8) & 0x0F];
            buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
            buf[j++] = HEX_DIGITS[k & 0x0F];
        }
        return new String(buf);
    }

    // main(): use to generate the Intermediate Values KAT
    // ...........................................................................

    /**
     *
     * @param args
     *            input parameters
     */
    public static void main(String[] args) {
        self_test(16);
        self_test(24);
        self_test(32);
    }
}
