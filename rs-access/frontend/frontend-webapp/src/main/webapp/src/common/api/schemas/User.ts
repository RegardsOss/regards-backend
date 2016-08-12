import { Schema, arrayOf } from "normalizr"
import { find } from "lodash"
import { MISSING_LINK } from "./common"
import ProjectUser from "./ProjectUser"
//  import { HATEOASLink } from './common'
/* const test: HATEOASLink =  {
 rel: "something",
 href: 'something else'
 }*/
const userSchema = new Schema('users', {
  idAttribute: (user) => {
    const itself: any = find(user.links, {"rel": "self"})
    return itself ? itself.href : MISSING_LINK
  }
})

userSchema.define({
  projectUsers: ProjectUser.PROJECT_USER_SCHEMA_ARRAY
})

// Schemas for API responses.
export default {
  USER_SCHEMA: userSchema,
  USER_SCHEMA_ARRAY: arrayOf(userSchema)
}
