import { HateosLink } from "../hateos/types"

export interface Account {
  accountId: number,
  firstName: string,
  lastName: string,
  login: string,
  password?: string,
  status: string,
  email: string,
  links: Array<HateosLink>
}
