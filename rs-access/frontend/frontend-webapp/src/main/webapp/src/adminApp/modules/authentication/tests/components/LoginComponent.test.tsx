import * as React from 'react'
import { shallow } from 'enzyme'
import { expect } from 'chai'
import * as sinon from 'sinon'
import LoginComponent from '../../components/LoginComponent';

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
      errorMessage: ''
    };
    const wrapper = shallow(<LoginComponent {...props}/>);
    /*
    expect(wrapper.find("Card")).to.have.length(1);
    expect(wrapper.find("p")).to.have.length(0);
    expect(wrapper.find("TextField")).to.have.length(2);
    expect(wrapper.find("RaisedButton")).to.have.length(1);
*/
    // Test onLogin action
    wrapper.find("RaisedButton").simulate('click');
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

    const wrapper = shallow(<LoginComponent {...props}/>);
    const username = wrapper.find("TextField[type='text']");
    username.simulate('change', { target: { value: 'test' } });
    const password = wrapper.find("TextField[type='password']");
    password.simulate('change', { target: { value: 'test_password' } });
    // Test onLogin action
    wrapper.find("RaisedButton").simulate('click');
    expect(spy.calledOnce).to.equals(true)
  });

});
