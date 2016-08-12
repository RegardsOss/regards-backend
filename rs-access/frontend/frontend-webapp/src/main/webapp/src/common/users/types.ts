export interface User {
  accountId: number,
  firstName: string,
  lastName: string,
  login: string,
  password?: string,
  status: string,
  email: string,
  links: [{
    href: string,
    rel: string
  }]
}
