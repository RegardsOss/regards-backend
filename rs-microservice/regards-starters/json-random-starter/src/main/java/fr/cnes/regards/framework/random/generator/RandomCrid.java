package fr.cnes.regards.framework.random.generator;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Locale;
import java.util.Random;

public class RandomCrid extends AbstractRandomGenerator<String>{

    public static Random random = new Random();

    public RandomCrid(FunctionDescriptor fd) {
        super(fd);
    }

    @Override
    public String random() {

        StringBuilder stringBuilder = new StringBuilder();
        String firstPool = "DPTVX";
        String secondPool = "GIO";

        stringBuilder
                .append(generateCharFromPool(firstPool, 1))
                .append(generateCharFromPool(secondPool, 1))
                .append(RandomStringUtils.randomAlphanumeric(2).toUpperCase(Locale.ROOT));
        return stringBuilder.toString();
    }

    /**
     * Create a random string from a character pool
     * @param pool characters pool
     * @param size result string size
     * @return random string
     */
    String generateCharFromPool(String pool, int size){

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < size; i++){
            int randomInt = random.nextInt(pool.length());
            builder.append(pool.charAt(randomInt));
        }
        return builder.toString();
    }
}
