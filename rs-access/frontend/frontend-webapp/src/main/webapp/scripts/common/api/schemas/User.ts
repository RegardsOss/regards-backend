import { Schema, arrayOf } from "normalizr";
import { find } from 'lodash'
import { HATEOASLink } from './common'
const test: HATEOASLink =  {
  rel: "something",
  href: 'something else'
}
const userSchema = new Schema ('user', {
  idAttribute: (user) => {
    const itself: any = find(user.links, {"rel": "self"})
    return itself ? itself.href : 'KEY_NOT_FOUND';
  }
})
/*
userSchema.define({
  admins: arrayOf(projectSchema),
  projects: arrayOf(projectSchema)
})*/

// Schemas for API responses.
export default {
  USER_SCHEMA: userSchema,
  USER_SCHEMA_ARRAY: arrayOf(userSchema)
}
