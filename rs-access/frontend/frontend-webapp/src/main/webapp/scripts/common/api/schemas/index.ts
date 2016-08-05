import UserSchema from "./User"
import ProjectSchema from "./Project"
import ProjectAdminSchema from './ProjectAdmin'
// We use this Normalizr schemas to transform API responses from a nested form
// to a flat form where repos and users are placed in `entities`, and nested
// JSON objects are replaced with their IDs. This is very convenient for
// consumption by reducers, because we can easily build a normalized tree
// and keep it updated as we fetch more data.

// Schemas for API responses.
export default {
  USER: UserSchema.USER_SCHEMA,
  USER_ARRAY: UserSchema.USER_SCHEMA_ARRAY,
  PROJECT: ProjectSchema.PROJECT,
  PROJECT_ARRAY: ProjectSchema.PROJECT_ARRAY,
  PROJECT_ADMIN: ProjectAdminSchema.PROJECT_ADMIN,
  PROJECT_ADMIN_ARRAY: ProjectAdminSchema.PROJECT_ADMIN_ARRAY
}

