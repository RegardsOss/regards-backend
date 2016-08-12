import { expect } from 'chai'
import { Action } from 'redux'

import reducer from '../../../src/common/i18n/I18nReducers';
import { SET_LOCALE, SET_LOCALE_MSG } from '../../../src/common/i18n/I18nActions'

describe('[COMMON] Testing i18n reducer', () => {

  it('should return the initial state', () => {
    expect(reducer(undefined, {})).to.eql({
      locale : 'en',
      messages: []
    })
  })

  it('should handle update locale success', () => {
    const action = {
      type: SET_LOCALE,
      locale: 'fr'
    }
    const initState = {
      locale : 'en',
      messages: [{
        messagesDir: '../tests/common/i18n/messages/test1',
        messages: { 'message1':'premier message' }
      },{
        messagesDir: '../tests/common/i18n/messages/test2',
        messages: { 'message2':'deuxieme message' }
      }]
    }
    const expectedState = {
      locale : 'fr',
      messages: [{
        messagesDir: '../tests/common/i18n/messages/test1',
        messages: { 'message1':'premier message' }
      },{
        messagesDir: '../tests/common/i18n/messages/test2',
        messages: { 'message2':'deuxieme message' }
      }]
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })


  it('should handle update messages success', () => {
    const action = {
      type: SET_LOCALE_MSG,
      messagesDir: '../tests/common/i18n/messages/test1',
      messages: {'message1':'nouveau premier message'}
    }
    const initState = {
      locale : 'fr',
      messages: [{
        messagesDir: '../tests/common/i18n/messages/test1',
        messages: { 'message1':'premier message' }
      },{
        messagesDir: '../tests/common/i18n/messages/test2',
        messages: { 'message2':'deuxieme message' }
      }]
    }
    const expectedState = {
      locale : 'fr',
      messages: [{
        messagesDir: '../tests/common/i18n/messages/test1',
        messages: { 'message1':'nouveau premier message' }
      },{
        messagesDir: '../tests/common/i18n/messages/test2',
        messages: { 'message2':'deuxieme message' }
      }]
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should handle new locales messages success', () => {
    const action = {
      type: SET_LOCALE_MSG,
      messagesDir: '../tests/common/i18n/messages/test3',
      messages: {'message3':'troisieme message'}
    }
    const initState = {
      locale : 'fr',
      messages: [{
        messagesDir: '../tests/common/i18n/messages/test1',
        messages: { 'message1':'premier message' }
      },{
        messagesDir: '../tests/common/i18n/messages/test2',
        messages: { 'message2':'deuxieme message' }
      }]
    }
    const expectedState = {
      locale : 'fr',
      messages: [{
        messagesDir: '../tests/common/i18n/messages/test1',
        messages: { 'message1':'premier message' }
      },{
        messagesDir: '../tests/common/i18n/messages/test2',
        messages: { 'message2':'deuxieme message' }
      },{
        messagesDir: '../tests/common/i18n/messages/test3',
        messages: { 'message3':'troisieme message' }
      }]
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })
})
