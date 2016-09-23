import { SelectTheme } from "../../../../../web_modules/view/theme/src/containers/SelectTheme";
import { storiesOf, linkTo, action } from "@kadira/storybook";
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"

storiesOf('Theme', module)
  .addDecorator(ThemeDecorator)
  .add('Select another theme', () => (
    <SelectTheme
      setTheme={action("clicked")}
      theme="darkBaseTheme"
    />
  ))

