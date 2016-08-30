import { Schema } from "normalizr"
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
