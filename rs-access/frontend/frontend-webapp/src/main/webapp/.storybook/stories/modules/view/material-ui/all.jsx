import { ProjectAccountCreateContainer } from "../../../../../web_modules/modules/admin-user-management/src/containers/ProjectAccountCreateContainer"
import { storiesOf, linkTo, action } from "@kadira/storybook";
import { StoreDecorator, ThemeDecorator } from "../../../utils/decorators"
import darkBaseTheme from "material-ui/styles/baseThemes/darkBaseTheme";
import RaisedButton from "./RaisedButton";
import AppBar from "./AppBar";
import Palette from "./Palette";
import lightBaseTheme from "material-ui/styles/baseThemes/lightBaseTheme";

storiesOf('Material UI', module)
  .add('Palette with lightBaseTheme', () => (
    <Palette theme={lightBaseTheme}/>
  ))
  .add('Palettewith darkBaseTheme', () => (
    <Palette theme={darkBaseTheme}/>
  ))
  .add('RaisedButton with lightBaseTheme', () => (
    <RaisedButton theme={lightBaseTheme}/>
  ))
  .add('RaisedButton with darkBaseTheme', () => (
    <RaisedButton theme={darkBaseTheme}/>
  ))
  .add('AppBar with lightBaseTheme', () => (
    <AppBar theme={lightBaseTheme}/>
  ))
  .add('AppBar with darkBaseTheme', () => (
    <AppBar theme={darkBaseTheme}/>
  ));
