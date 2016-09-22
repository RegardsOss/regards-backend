import MuiThemeProvider from "material-ui/styles/MuiThemeProvider";
import { Provider } from "react-redux";
import { configureStore } from "@regardsoss/store";
import rootReducer from "../../../src/rootReducer";
const store = configureStore(rootReducer)
import getMuiTheme from "material-ui/styles/getMuiTheme";

export const ThemeDecorator = (story) => (
  <MuiThemeProvider muiTheme={getMuiTheme("light")}>
    {story()}
  </MuiThemeProvider>
);

export const StoreDecorator = (story) => (
  <Provider store={store}>
    {story()}
  </Provider>
);
