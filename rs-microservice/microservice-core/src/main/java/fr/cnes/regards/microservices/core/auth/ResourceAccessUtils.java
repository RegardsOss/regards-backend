package fr.cnes.regards.microservices.core.auth;

public class ResourceAccessUtils {

	private static final String SEPARATOR = "@";

	public static String getIdentifier(ResourceAccess access) {
		if (access != null) {
			return access.name() + SEPARATOR + access.method();
		}
		return null;
	}

	public static String getIdentifier(String resourceName, String httpMethod) {
		if (resourceName != null && httpMethod != null) {
			return resourceName + SEPARATOR + httpMethod;
		}
		return null;
	}
}
