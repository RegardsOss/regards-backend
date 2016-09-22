import { ProjectAccountCreateContainer } from "../../../../../web_modules/modules/admin-user-management/src/containers/ProjectAccountCreateContainer"
import { storiesOf, linkTo, action } from "@kadira/storybook";
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"

storiesOf('User Admin Management: Create ProjectAccount', module)
  .addDecorator(ThemeDecorator)
  .add('', () => (
    <ProjectAccountCreateContainer />
  ))


