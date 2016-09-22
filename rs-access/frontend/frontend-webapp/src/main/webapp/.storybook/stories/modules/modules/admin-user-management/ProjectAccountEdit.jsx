import { ProjectAccountEditContainer } from "../../../../../web_modules/modules/admin-user-management/src/containers/ProjectAccountEditContainer"

import { storiesOf, linkTo, action } from "@kadira/storybook";
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"

storiesOf('User Admin Management: Edit ProjectAccount', module)
  .addDecorator(ThemeDecorator)
  .add('', () => (
    <ProjectAccountEditContainer />
  ))


