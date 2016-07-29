import { Schema, arrayOf } from "normalizr";

// We use this Normalizr schemas to transform API responses from a nested form
// to a flat form where repos and users are placed in `entities`, and nested
// JSON objects are replaced with their IDs. This is very convenient for
// consumption by reducers, because we can easily build a normalized tree
// and keep it updated as we fetch more data.

// Read more about Normalizr: https://github.com/paularmstrong/normalizr
const projectSchema = new Schema ('projects', {
  idAttribute: project => project.links[0].href
})

const projectAdminSchema = new Schema ('projectAdmins', {
  idAttribute: projectAdmin => projectAdmin.links[0].href
})

projectSchema.define ({
  admins: arrayOf (projectAdminSchema)
})

projectAdminSchema.define ({
  projects: arrayOf (projectSchema)
})

// Schemas for API responses.
export default {
  PROJECT: projectSchema,
  PROJECT_ARRAY: arrayOf (projectSchema),
  PROJECT_ADMIN: projectAdminSchema,
  PROJECT_ADMIN_ARRAY: arrayOf (projectAdminSchema)
}
