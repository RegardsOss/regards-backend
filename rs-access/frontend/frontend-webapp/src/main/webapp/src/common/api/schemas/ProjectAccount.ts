import { Schema, arrayOf } from "normalizr"
import Role from "./Role"
import Account from "./Account"
import { ProjectAccount } from "../../models/users/types"

const NAME = 'projectAccounts'
const projectAccountSchema = new Schema(NAME, {
  idAttribute: (projectAccount: ProjectAccount) => {
    return projectAccount.projectAccountId
  }
})


// Schemas for API responses.
export default {
  PROJECT_ACCOUNT_SCHEMA: projectAccountSchema
}
