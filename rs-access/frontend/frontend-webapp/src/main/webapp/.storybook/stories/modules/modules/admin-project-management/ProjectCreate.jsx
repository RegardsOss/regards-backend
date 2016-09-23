import { storiesOf, linkTo, action } from "@kadira/storybook";
import { ProjectCreateContainer } from "../../../../../web_modules/modules/admin-project-management/src/containers/ProjectCreateContainer"
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"

storiesOf('Project Admin Management: Create Project', module)
  .addDecorator(StoreDecorator)
  .addDecorator(ThemeDecorator)
  .add('', () => (
    <ProjectCreateContainer
      params={{projectName: "cdpp"}}
      project={[{name: "john", projectId: "john", description: "doe", isPublic: false, links: [], icon: "http://lorempicsum.com/futurama/350/200/1"}]}
      theme=""
      createProject={action("createProject")}
    />
  ))

