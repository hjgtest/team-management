<template>
  <div>Test Component</div>
</template>

<script>
export default {
  name: 'TestComponent',
  data() {
    return {
      test: 'hello'
    }
  },
  methods: {
    testMethod1() {
      return 'test1';
    },
    testMethod2() {
      return 'test2';
    }
  }
}
</script>