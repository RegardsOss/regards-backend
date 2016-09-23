import { storiesOf, linkTo, action } from "@kadira/storybook";
import { ProjectsContainer } from "../../../../../web_modules/modules/admin-project-management/src/containers/ProjectsContainer"
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"

storiesOf('Project Admin Management: List Project', module)
  .addDecorator(StoreDecorator)
  .addDecorator(ThemeDecorator)
  .add('', () => (
    <ProjectsContainer
      project={[{name: "john", projectId: "john", description: "doe", isPublic: false, links: [], icon: "http://lorempicsum.com/futurama/350/200/1"}]}
      theme=""
      createProject={action("createProject")}
      fetchProjects={action("fetchProjects")}
      deleteProject={action("deleteProject")}
    />
  ))

