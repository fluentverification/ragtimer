# Export Options

## PRISM Explicit Model

The PRISM explicit model export produces three files: `prism.sta`, `prism.tra`, and `prism.lab`.
These represent, respectively, state information, transition information, and state labling information.

To check an explicit model against a property stored in `pro.csl` in PRISM, run the following command:

```
prism -importmodel prism.tra,sta,lab -ctmc pro.csl
```

## STORM Explicit Model

The STORM explicit model export produces two files: `storm.tra` and `storm.lab`, or a transition
and label information file. No state information is provided, since the target states are all 
indicated in the label file. That is, the label file indicates a list of target states.

To check an explicit model for target state reachability, assuming the property from before
and a time bound of `200`, run the following command:

```
storm --explicit storm.tra storm.lab --prop 'P=? [true U[0,200] "target"]'
```