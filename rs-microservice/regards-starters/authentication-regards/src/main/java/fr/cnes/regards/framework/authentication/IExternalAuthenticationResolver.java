package fr.cnes.regards.framework.authentication;

public interface IExternalAuthenticationResolver {

    String verifyAndAuthenticate(String externalToken);
}
