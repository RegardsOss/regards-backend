import { Schema, arrayOf } from "normalizr"
import { find } from "lodash"
import Role from "./Role"
import { MISSING_LINK } from "./common"

const NAME = 'projectUsers'
const projectUserSchema = new Schema(NAME, {
  idAttribute: (projectUser) => {
    const itself: any = find(projectUser.links, {"rel": "self"})
    return itself ? itself.href : MISSING_LINK
  }
})


projectUserSchema.define({
  role: Role.ROLE_SCHEMA
})

// Schemas for API responses.
export default {
  PROJECT_USER_SCHEMA: projectUserSchema,
  PROJECT_USER_SCHEMA_ARRAY: arrayOf(projectUserSchema)
}

