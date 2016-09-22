import { ProjectAccountsContainer } from "../../../../../web_modules/modules/admin-user-management/src/containers/ProjectAccountsContainer"
import { storiesOf, linkTo, action } from "@kadira/storybook";
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"


storiesOf('User Admin Management: List ProjectAccounts', module)
  .addDecorator(StoreDecorator)
  .addDecorator(ThemeDecorator)
  .add('', () => (
    <ProjectAccountsContainer
      params={{projectName: "cdpp"}}
      accounts={[{login: "john", firstName: "john", lastName: "doe", email: "john.doe@yopmail.com", status: 1}]}
      fetchProjectAccounts={action("fetchProjectAccounts")}
      deleteProjectAccount={action("deleteProjectAccount")}
    />
  ))

