package fr.cnes.regards.modules.accessrights.domain.registration;

import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;

/**
 * Dto class wrapping data required for both account and project user creation.
 *
 * @author Xavier-Alexandre Brochard
 */
public class AccessRequestDto {

    @Valid
    @NotBlank
    @Length(max = 128)
    @Email
    private String email;

    @Valid
    @Length(max = 128)
    private String firstName;

    @Valid
    @Length(max = 128)
    private String lastName;

    private String roleName;

    @Valid
    private List<MetaData> metadata;

    @Valid
    @Length(max = 255)
    private String password;

    private String originUrl;

    private String requestLink;

    private String origin;

    private Set<String> accessGroups;

    private Long maxQuota;

    public AccessRequestDto() {
    }

    public AccessRequestDto(String email, String firstName, String lastName, String roleName, List<MetaData> metadata, String password, String originUrl, String requestLink,
            String origin, Set<String> accessGroups, Long maxQuota
    ) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roleName = roleName;
        this.metadata = metadata;
        this.password = password;
        this.originUrl = originUrl;
        this.requestLink = requestLink;
        this.origin = origin;
        this.accessGroups = accessGroups;
        this.maxQuota = maxQuota;
    }

    public String getEmail() {
        return email;
    }

    public AccessRequestDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public AccessRequestDto setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public AccessRequestDto setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public AccessRequestDto setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public List<MetaData> getMetadata() {
        return metadata;
    }

    public AccessRequestDto setMetadata(List<MetaData> metadata) {
        this.metadata = metadata;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AccessRequestDto setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public AccessRequestDto setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
        return this;
    }

    public String getRequestLink() {
        return requestLink;
    }

    public AccessRequestDto setRequestLink(String requestLink) {
        this.requestLink = requestLink;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public AccessRequestDto setOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    public Set<String> getAccessGroups() {
        return accessGroups;
    }

    public AccessRequestDto setAccessGroups(Set<String> accessGroups) {
        this.accessGroups = accessGroups;
        return this;
    }

    public Long getMaxQuota() {
        return maxQuota;
    }

    public AccessRequestDto setMaxQuota(Long maxQuota) {
        this.maxQuota = maxQuota;
        return this;
    }

}
