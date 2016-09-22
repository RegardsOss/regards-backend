import { CardActionsComponent } from "../../../../../web_modules/view/components/src/main";
import { storiesOf, linkTo, action } from "@kadira/storybook";
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"
import darkBaseTheme from "material-ui/styles/baseThemes/darkBaseTheme";




storiesOf('Generic components', module)
  .addDecorator(ThemeDecorator)
  .add('[light] main button', () => (
    <CardActionsComponent
      mainButtonLabel="Main button"
      theme={darkBaseTheme}
    />
  ))
  .add('[light] main button & secondary button', () => (
    <CardActionsComponent
      mainButtonLabel="Main button"
      secondaryButtonLabel="Secondary button"
      secondaryButtonUrl="#"
      theme={darkBaseTheme}
    />
  ))
