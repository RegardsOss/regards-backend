import { HateoasLink } from "../hateoas/types"

export interface Account {
  accountId: number,
  firstName: string,
  lastName: string,
  login: string,
  password?: string,
  status: string,
  email: string,
  links: Array<HateoasLink>
}
