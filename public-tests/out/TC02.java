class TC02 {
    public static void main(String[] args) {
        TestTC02 o;
        int res;
        o = new TestTC02();
        res = o.foo();
        System.out.println(res);
    }
}
class TestTC02 {

    public int bar(int a, int b, int c) {
        a = b + c;
        return a;
    }

    public int foo() {
        int a;
        int b;
        int c;
        int d;
        boolean z;
        TestTC02 o;
        int TestTC02_bar_a;
        int TestTC02_bar_b;
        int TestTC02_bar_c;
        a = 1;
        b = 2;
        o = new TestTC02();
        c = 3;
        d = 4;
        TestTC02_bar_a = 1;
        TestTC02_bar_b = 2;
        TestTC02_bar_c = 3;
        TestTC02_bar_a = 5;
        d = 5;
        return 5;
    }
}
