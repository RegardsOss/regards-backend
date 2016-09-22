import { storiesOf, linkTo, action } from "@kadira/storybook";
import { ProjectReadContainer } from "../../../../../web_modules/modules/admin-project-management/src/containers/ProjectReadContainer"
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"

storiesOf('Project Admin Management: Read Project', module)
  .addDecorator(StoreDecorator)
  .addDecorator(ThemeDecorator)
  .add('', () => (
    <ProjectReadContainer
      params={{projectName: "cdpp"}}
      projects={[{name: "john", projectId: "john", description: "doe", isPublic: false, links: [], icon: "http://lorempicsum.com/futurama/350/200/1"}]}
      theme=""
    />
  ))

