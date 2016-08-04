import * as React from 'react'
import { shallow } from 'enzyme'
import { expect } from 'chai'
import * as sinon from 'sinon'
import MenuItem from "material-ui/MenuItem"
import SelectLocaleComponent from '../../../scripts/common/i18n/components/SelectLocaleComponent'

// Test a component rendering

describe('[COMMON] Testing i18n Select Locale component', () => {
  it('Should render correctly the SelectLocaleComponent', () => {
    const onLocaleChange = (locale:string) => { expect(locale).to.equals('es') };
    var spy = sinon.spy(onLocaleChange);
    let props = {
      setLocale: spy,
      curentLocale: 'ru',
      locales: ['fr','en','ru','es']
    }

    const context = {
      intl : {
        formatMessage : (message:any) => message.id
      }
    }

    const wrapper = shallow(<SelectLocaleComponent {...props}/>, { context })
    expect(wrapper.find(MenuItem)).to.have.length(4);
  })

})
