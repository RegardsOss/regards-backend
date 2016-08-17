import { Schema, arrayOf } from "normalizr"
import Role from "./Role"
import { ProjectAccount } from '../../models/users/types'

const NAME = 'projectAccounts'
const projectAccountSchema = new Schema(NAME, {
  idAttribute: (projectAccount:ProjectAccount) => {
    return projectAccount.projectAccountId
  }
})

projectAccountSchema.define({
  role: Role.ROLE_SCHEMA
})

// Schemas for API responses.
export default {
  PROJECT_ACCOUNT_SCHEMA: projectAccountSchema,
  PROJECT_ACCOUNT_SCHEMA_ARRAY: arrayOf(projectAccountSchema)
}
