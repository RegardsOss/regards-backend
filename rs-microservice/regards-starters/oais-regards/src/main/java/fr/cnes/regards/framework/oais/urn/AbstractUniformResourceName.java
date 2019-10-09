/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.oais.urn;

import java.util.StringJoiner;
import java.util.UUID;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * allow us to create URN
 *
 * @author Kevin Marchois
 */
public class AbstractUniformResourceName<T> {

	public static final int MAX_SIZE = 128;

	/**
	 * URN pattern
	 */
	public static final String URN_PATTERN = "URN:[^:]+:[^:]+:[^:]+:[^:]+:V\\d{1,3}(,\\d+)?(:REV.+)?";

	/**
	 * Version prefix
	 */
	private static final String VERSION_PREFIX = "V";

	/**
	 * Revision prefix
	 */
	private static final String REVISION_PREFIX = "REV";

	/**
	 * Version minimum value
	 */
	private static final int MIN_VERSION_VALUE = 1;

	/**
	 * Version maximum value
	 */
	private static final int MAX_VERSION_VALUE = 999;

	/**
	 * the identifier
	 */
	@NotNull
	private T identifier;

	/**
	 * Entity type
	 */
	@NotNull
	private EntityType entityType;

	/**
	 * Tenant which the entity belongs toù
	 */
	@NotNull
	private String tenant;

	/**
	 * Entity id
	 */
	@NotNull
	private UUID entityId;

	/**
	 * Entity version number on 3 digits by specs(cf REGARDS_DSL_SYS_ARC_410)
	 */
	@Min(MIN_VERSION_VALUE)
	@Max(MAX_VERSION_VALUE)
	private int version;

	/**
	 * numeric value ordering the differents AIP from a same SIP
	 */
	private Long order;

	/**
	 * Revision of the entity
	 */
	private String revision;

	/**
	 * Constructor setting the given parameters as attributes
	 */
	public AbstractUniformResourceName(T identifier, EntityType entityType, String tenant, UUID entityId, int version) {
		super();
		this.identifier = identifier;
		this.entityType = entityType;
		this.tenant = tenant;
		this.entityId = entityId;
		this.version = version;
	}

	/**
	 * Constructor setting the given parameters as attributes
	 */
	public AbstractUniformResourceName(T identifier, EntityType entityType, String tenant, UUID entityId, int version,
			Long order, String revision) {
		this(identifier, entityType, tenant, entityId, version);
		this.order = order;
		this.revision = revision;
	}

	/**
	 * Constructor setting the given parameters as attributes
	 */
	public AbstractUniformResourceName(T identifier, EntityType entityType, String tenant, UUID entityId, int version,
			long order) {
		this(identifier, entityType, tenant, entityId, version);
		this.order = order;
	}

	/**
	 * Constructor setting the given parameters as attributes
	 */
	public AbstractUniformResourceName(T identifier, EntityType entityType, String tenant, UUID entityId, int version,
			String revision) {
		this(identifier, entityType, tenant, entityId, version);
		this.revision = revision;
	}

	public AbstractUniformResourceName() {
		// for testing purpose
	}

	@Override
	public String toString() {
		final StringJoiner urnBuilder = new StringJoiner(":", "URN:", "");
		urnBuilder.add(identifier.toString());
		urnBuilder.add(entityType.toString());
		urnBuilder.add(tenant);
		urnBuilder.add(entityId.toString());
		String orderString = "";
		if (order != null) {
			orderString = "," + order;
		}
		// order is not added with the joiner because it is "version,order" and not
		// "version:order"
		urnBuilder.add(VERSION_PREFIX + version + orderString);
		if (revision != null) {
			urnBuilder.add(REVISION_PREFIX + revision);
		}
		return urnBuilder.toString();
	}

	/**
	 * @return the identifier
	 */
	public T getIdentifier() {
		return identifier;
	}

	/**
	 * Set the identifier
	 */
	public void setIdentifier(T identifier) {
		this.identifier = identifier;
	}

	/**
	 * @return the entity type
	 */
	public EntityType getEntityType() {
		return entityType;
	}

	/**
	 * Set the entity type
	 */
	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	/**
	 * @return the tenant
	 */
	public String getTenant() {
		return tenant;
	}

	/**
	 * Set the tenant
	 */
	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	/**
	 * @return the entity id
	 */
	public UUID getEntityId() {
		return entityId;
	}

	/**
	 * Set the entity id
	 */
	public void setEntityId(UUID entityId) {
		this.entityId = entityId;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Set the version
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the order
	 */
	public Long getOrder() {
		return order;
	}

	/**
	 * Set the order
	 */
	public void setOrder(Long order) {
		this.order = order;
	}

	/**
	 * @return the revision
	 */
	public String getRevision() {
		return revision;
	}

	/**
	 * Set the revision
	 */
	public void setRevision(String revision) {
		this.revision = revision;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}

		AbstractUniformResourceName that = (AbstractUniformResourceName) o;

		if (version != that.version) {
			return false;
		}
		if (identifier != that.identifier) {
			return false;
		}
		if (entityType != that.entityType) {
			return false;
		}
		if (!tenant.equals(that.tenant)) {
			return false;
		}
		if (!entityId.equals(that.entityId)) {
			return false;
		}
		if (order != null ? !order.equals(that.order) : that.order != null) {
			return false;
		}
		return revision != null ? revision.equals(that.revision) : that.revision == null;
	}

	@Override
	public int hashCode() {
		int result = identifier.hashCode();
		result = (31 * result) + entityType.hashCode();
		result = (31 * result) + tenant.hashCode();
		result = (31 * result) + entityId.hashCode();
		result = (31 * result) + version;
		result = (31 * result) + (order != null ? order.hashCode() : 0);
		result = (31 * result) + (revision != null ? revision.hashCode() : 0);
		return result;
	}
}
