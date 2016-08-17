import { Schema, arrayOf } from "normalizr"
import { find } from "lodash"
import projectAccountSchema from "./ProjectAccount"
import { Account } from "../../models/users/types"

const accountSchema = new Schema('accounts', {
  idAttribute: (account:Account) => {
    return account.accountId
  }
})

accountSchema.define({
  projectAccounts: projectAccountSchema.PROJECT_ACCOUNT_SCHEMA_ARRAY
})

// Schemas for API responses.
export default {
  ACOUNT_SCHEMA: accountSchema,
  ACCOUNT_SCHEMA_ARRAY: arrayOf(accountSchema)
}
