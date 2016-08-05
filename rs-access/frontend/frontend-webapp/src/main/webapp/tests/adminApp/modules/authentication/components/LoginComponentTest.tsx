import * as React from 'react'
import { mount } from 'enzyme'
import { expect } from 'chai'
import * as sinon from 'sinon'
import LoginComponent from '../../../../../scripts/adminApp/modules/authentication/components/LoginComponent';


// Test a component rendering

describe('[ADMIN APP] Testing login component', () => {
  it('Should render correctly the login component', () => {
    const onLogin = (username:string, password:string) => { };
    const spy = sinon.spy(onLogin)
    const loginStyles:any = {
      "login-modal": 'login-modal',
      "login-error": "login-error"
    };
    let props = {
      onLogin: spy,
      errorMessage: '',
      muiTheme: '',
      store: {}
    };

    const wrapper = mount(<LoginComponent {...props}/>);
    console.log(wrapper)
    expect(wrapper.find("button")).to.have.length(1);

    expect(wrapper.find("div")).to.have.length(1);
    expect(wrapper.find("input[type='text']")).to.have.length(1);
    expect(wrapper.find("input[type='password']")).to.have.length(1);

    // Test onLogin action
    wrapper.find("button").simulate('click');
    console.log(wrapper.find("button"))
    expect(spy.calledOnce).to.equals(true)
  });

  it('Should active login action correctly', () => {
    const onLogin = (username:string, password:string) => {
      expect(username).to.equal("test");
      expect(password).to.equal("test_password");
    };
    const spy = sinon.spy(onLogin)
    const handleKeyPress = () => { };
    let props = {
      onLogin: spy,
      errorMessage: ''
    };

    const wrapper = mount(<LoginComponent {...props}/>);
    const username = wrapper.find("input[type='text']");
    username.simulate('change', { target: { value: 'test' } });
    const password = wrapper.find("input[type='password']");
    password.simulate('change', { target: { value: 'test_password' } });
    // Test onLogin action
    wrapper.find("button").simulate('click');
    expect(spy.calledOnce).to.equals(true)
  });

});
