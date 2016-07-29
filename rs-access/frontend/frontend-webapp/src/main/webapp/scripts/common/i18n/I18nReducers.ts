import { SET_LOCALE, SET_LOCALE_MSG } from "./I18nActions";
import { i18nStore, localeMessagesStore } from "./I18nTypes";
// If navigator is not defined, set the locale to english
if (typeof navigator === 'undefined')  var navigator: any = {language: 'en'}


export default (state: i18nStore = {
  locale: navigator.language,
  messages: []
}, action: any) => {
  switch (action.type) {
    // Running fetch plugins from server
    case SET_LOCALE:
      return Object.assign ({}, state, {locale: action.locale})
    case SET_LOCALE_MSG:
      // Duplicate state
      let newState = Object.assign ({}, state)
      let newMessages: Array<localeMessagesStore> = Object.assign ([], state.messages)
      // Find message associated to the messagesDir of the action
      let localeMessages: localeMessagesStore = newMessages.find ((message) => message.messagesDir === action.messagesDir)
      // If the messageDir already define, juste update the messages with the new ones
      if (localeMessages) {
        localeMessages.messages = action.messages
      } else {
        // Else, create a new messagedir object
        newMessages.push ({
          messagesDir: action.messagesDir,
          messages: action.messages
        })
      }
      newState.messages = newMessages
      return newState
    default:
      return state
  }
}
