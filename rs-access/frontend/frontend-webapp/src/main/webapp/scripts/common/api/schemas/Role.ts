import { Schema, arrayOf } from "normalizr"
import { find } from "lodash"
import { MISSING_LINK } from "./common"

const NAME = "roles"
const roleSchema = new Schema(NAME, {
  idAttribute: (role) => {
    const itself: any = find(role.links, {"rel": "self"})
    return itself ? itself.href : MISSING_LINK
  }
})


// Schemas for API responses.
export default {
  ROLE_SCHEMA: roleSchema,
  ROLE_SCHEMA_ARRAY: arrayOf(roleSchema)
}

