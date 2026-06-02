// 简单的语法测试
const testFunction = function() {
  console.log('Basic syntax test passed');
  return true;
};

// 测试对象方法定义
const testObj = {
  method1() {
    return 'method1 works';
  },
  method2() {
    return 'method2 works';
  }
};

console.log(testFunction());
console.log(testObj.method1());
console.log(testObj.method2());

// 测试ES6箭头函数
const arrowFunc = () => {
  return 'arrow function works';
};

console.log(arrowFunc());