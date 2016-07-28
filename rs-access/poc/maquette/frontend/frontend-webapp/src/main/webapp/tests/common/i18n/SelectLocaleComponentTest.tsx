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
    let props = {
      onLocaleChange: spy,
      curentLocale: 'ru',
      locales: ['fr','en','ru','es']
    }

    const wrapper = shallow(<SelectLocaleComponent {...props}/>)
    expect(wrapper.find("div")).to.have.length(1)
    expect(wrapper.find("div select")).to.have.length(1)
    expect(wrapper.find("div select option")).to.have.length(4)
    const select:any = wrapper.find("div select")
    expect(select.props().value).to.equals("ru")
    select.simulate('change',{ target: { value: 'es' }})
    expect(spy.calledOnce).to.equals(true)
  })

})
