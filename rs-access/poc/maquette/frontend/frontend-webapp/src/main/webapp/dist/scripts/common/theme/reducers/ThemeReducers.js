"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = '', action) => {
    switch (action.type) {
        case "SET_THEME":
            return action.theme;
        default:
            return state;
    }
};
// const themeReducers = {
//   theme
// }
//
// export default themeReducers
//# sourceMappingURL=ThemeReducers.js.map