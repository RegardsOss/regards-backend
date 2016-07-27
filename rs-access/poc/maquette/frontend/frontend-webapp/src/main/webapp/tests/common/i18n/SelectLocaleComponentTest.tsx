import * as React from 'react'
import { shallow } from 'enzyme'
import { expect } from 'chai'
import * as sinon from 'sinon'
import { SelectLocaleComponent } from '../../../scripts/common/i18n/SelectLocaleComponent'

// Test a component rendering

describe('[COMMON] Testing i18n Select Locale component', () => {
  it('Should render correctly the SelectLocaleComponent', () => {
    const onLocaleChange = (locale:string) => { expect(locale).to.equals('es') };
    var spy = sinon.spy(onLocaleChange);
    const styles:any = {
      "select-language": 'select-language'
    }
    let props = {
      onLocaleChange: spy,
      styles: styles,
      curentLocale: 'ru',
      locales: ['fr','en','ru','es']
    }

    const wrapper = shallow(<SelectLocaleComponent {...props}/>)
    expect(wrapper.find("div .select-language")).to.have.length(1)
    expect(wrapper.find("div .select-language select")).to.have.length(1)
    expect(wrapper.find("div .select-language select option")).to.have.length(4)
    const select:any = wrapper.find("div .select-language select")
    expect(select.props().value).to.equals("ru")
    select.simulate('change',{ target: { value: 'es' }})
    expect(spy.calledOnce).to.equals(true)
  })

})
