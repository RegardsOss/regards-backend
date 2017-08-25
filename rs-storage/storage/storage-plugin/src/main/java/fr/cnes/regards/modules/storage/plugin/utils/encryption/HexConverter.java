package fr.cnes.regards.modules.storage.plugin.utils.encryption;

/**
 * La classe <code>BinaryConverter</code> offre des fonctions statiques de conversion d'octets en code hexadecimaux
 *
 * @author  CS
 * @version 1.0
 *
 * @since 1.0
 */
public class HexConverter {

    /**
     * Les 16 valeurs de cle du code hexa
     * @since 1.0
     */
    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F' };

    /**
     * Convertit un octet en sa valeur hexadecimale ("0xXX").
     * @return la valeur hexadecimale
     * @param pPrefixed prefixe ?
     * @param pValue la valeur de l'octet a convertir
     */
    protected static String byteToHexadecimal(byte pValue, boolean pPrefixed) {

        String resultR = null;
        String resultL = null;
        String prefix = null;
        if (pPrefixed) {
            prefix = "0x";
        } else {
            prefix = "";
        }
        String[] letters = { "A", "B", "C", "D", "E", "F" };
        byte right = (byte) (pValue & (byte) (15));
        byte left = (byte) ((byte) (pValue >> 4) & (byte) (15));
        if (right < 10) {
            resultR = Byte.toString(right);
        } else {
            resultR = letters[right - 10];
        }
        if (left < 10) {
            resultL = Byte.toString(left);
        } else {
            resultL = letters[left - 10];
        }
        return (prefix.concat(resultL.concat(resultR)));
    }

    /**
     * Convertit une valeur hexadecimale en valeur octet
     *
     * @param pValue la valeur hexadecimale a convertir ("XX").
     * @return la valeur en octet
     **/
    protected static byte hexadecimalToByte(String pValue) {
        byte result = 0;
        char[] listChar = pValue.toCharArray();
        byte left = (byte) (Integer.parseInt(String.valueOf(listChar[0]), 16));
        byte right = (byte) (Integer.parseInt(String.valueOf(listChar[1]), 16));
        left = (byte) ((byte) (left << 4) & (byte) (-16));
        right = (byte) (right & (byte) (15));
        result = (byte) (left | right);
        return result;
    }

    /**
    * Convertit une valeur hexadecimale en valeur octet
    *
    * @param pValue la valeur hexadecimale a convertir.
    * @return la valeur en octet
    **/
    protected static byte[] hexadecimalToArrayOfByte(String pValue) {
        String data = new String(pValue);
        char[] listCharTemp = pValue.toCharArray();
        int valueLength = listCharTemp.length;
        if ((valueLength % 2) == 1) {
            data = "0" + data;
            valueLength++;
        }
        byte[] result = new byte[valueLength / 2];
        for (int i = 0; i < valueLength; i = i + 2) {
            result[i / 2] = hexadecimalToByte(data.substring(i, i + 2));
        }
        return result;
    }

    /**
     * Compare the valeurs hexadecimales.
     * les valeurs sont considerees comme etant positives.
     *
     * @param pParam1 la premiere valeur
     * @param pParam2 la deuxieme valeur
     * @return vrai si pParam1 >= pParam2, faux sinon.
     */
    protected static boolean compareHexadecimal(String pParam1, String pParam2) {
        char[] param1Char = pParam1.toCharArray();
        char[] param2Char = pParam2.toCharArray();
        int difference = param1Char.length - param2Char.length;
        if (difference != 0) {
            if (difference > 0) {
                for (int i = 0; i < difference; i++) {
                    if (param1Char[i] != '0') {
                        return true;
                    }
                }
                return compareHexadecimal(new String(param1Char, difference, param2Char.length), pParam2);
            } else {
                for (int i = 0; i < (difference * (-1)); i++) {

                    if (param2Char[i] != '0') {
                        return false;
                    }
                }
                return compareHexadecimal(pParam1, new String(param2Char, difference * (-1), param1Char.length));
            }
        } else {
            for (int i = 0; i < param1Char.length; i++) {
                char[] tmp1 = { '0', param1Char[i] };
                String value1 = new String(new String(tmp1));
                char[] tmp2 = { '0', param2Char[i] };
                String value2 = new String(new String(tmp2));
                byte result1 = hexadecimalToByte(value1);
                byte result2 = hexadecimalToByte(value2);
                if (result1 != result2) {
                    return (result1 > result2);
                }
            }
            return false;
        }
    }

    /**
     * Convertit un hexadecimal en unsigned double
     * @return la valeur en octet
     * @param pValue l'hexadecimal a convertir : 8 octets maximum a convertir ("XXXXXXXXXXXXXXXX");.
     * @throws java.lang.NumberFormatException Exception survnue lors du traitement
     */
    protected static double hexadecimalUnsignedDoubleValue(String pValue) throws NumberFormatException {
        double result = 0;
        char[] listChar = pValue.toCharArray();
        if (listChar.length > 16) {
            throw new NumberFormatException("value of hexadecimalUnsignedDoubleValue is too long");
        } else {
            for (int i = 0; i < listChar.length; i++) {
                char[] tmp = { '0', listChar[i] };
                String valueTmp = new String(new String(tmp));
                byte resultTmp = hexadecimalToByte(valueTmp);
                result = result + resultTmp * Math.pow(2, (listChar.length - 1 - i) * 4);
            }
            return result;
        }
    }

    /**
     * Retourne une expression textuelle d'un champ d'octets en hexadecimal
     *
     * @param pByte le tableau d'octets
     * @return la chaine de caractere contenant la representation hexadecimale
     */
    protected static String toString(byte[] pByte) {
        int length = pByte.length;
        char[] buf = new char[length * 2];
        int j = 0;
        int k = 0;
        for (int i = 0; i < length;) {
            k = pByte[i++];
            buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
            buf[j++] = HEX_DIGITS[k & 0x0F];
        }
        return new String(buf);
    }

}
