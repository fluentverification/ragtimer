class ctrl_state {
    var ID: int
    var init: bool
    var transitions: seq<transition>

    constructor(ID_in: int, init_in: bool)
        requires ID_in >= 0
        ensures ID == ID_in && init == init_in && transitions == []
    {
        ID := ID_in;
        init := init_in;
        transitions := [];
    }

    method add_trans(transition_in: transition)
        modifies this
        ensures this.transitions == old(this.transitions) + [transition_in]
    {
        this.transitions := this.transitions + [transition_in];
    }

}
class transition {
    var ID: nat
    var src: ctrl_state
    var dest: ctrl_state
    var cnt_guard: seq<int>
    var species_guard: seq<int>

    constructor(ID_in: nat, src_in: ctrl_state, dest_in: ctrl_state, cnt_grd: seq<int>, species_grd: seq<int>)
        requires ID_in >= 0
        requires ID_in < |cnt_grd|
        ensures ID < |cnt_guard|
        ensures ID == ID_in && cnt_guard == cnt_grd && species_guard == species_grd && src_in == src && dest_in == dest //add pre condition for same length for arrays
    {
        ID := ID_in;
        src := src_in;
        dest := dest_in;
        cnt_guard := cnt_grd;
        species_guard := species_grd;
    }
}


//abstract_state keeps track of current monitored species values, counter values, and prev_transitions in the "trace"
class abstract_state {
    //change to be nat instead of int
    var cnt: seq<int>
    var species: seq<int>
    var prev_reactions: seq<transition>

    constructor init(init_cnt: seq<int>, init_species: seq<int>)
        ensures cnt == init_cnt && species == init_species && prev_reactions == []
        ensures |cnt| == |init_cnt| && |species| == |init_species|
    {
        cnt := init_cnt;
        species := init_species;
        prev_reactions := [];
    }

    //TODO: errors on implementation.....
    constructor succ(prev: abstract_state, tran: transition)
        //requires # of counters and species is same for transition and actual counter and they are > 0
        requires |tran.cnt_guard| == |prev.cnt| && |tran.species_guard| == |prev.species| && |prev.cnt| > 0 && |prev.species| > 0
        //length of species and cnt guard is > 0
        requires |tran.species_guard| > 0 && |tran.cnt_guard| > 0
        //enough counters to take transition
        requires forall i :: 0 <= i < |tran.cnt_guard| ==> prev.cnt[i] >= tran.cnt_guard[i]
        //enough species to take transition
        requires forall i :: 0 <= i < |tran.species_guard| ==> prev.species[i] >= tran.species_guard[i]
        //ensures new cnt and new species have the same length that they had before
        ensures |cnt| == |prev.cnt| && |species| == |prev.species|
        //transistion list increases to have the new transtion
        ensures prev_reactions == old(prev_reactions) + [tran]
        //counters don't go below 0
        ensures forall i :: 0 <= i < |cnt| ==> cnt[i] >= 0
        //species don't go below 0
        ensures forall i :: 0 <= i < |species| ==> species[i] >= 0
        //Errors on implementation (haven't been able to figure them out)
    // {
    //     prev_reactions := prev.prev_reactions + [tran];
    //     var i : int := 0;
    //     cnt := [];
    //     species := [];
    //     new;
    //     cnt := seq(|prev.cnt|, i => (prev.cnt[i] - tran.cnt_guard[i]));
    //     species := prev.species;
    //     // new;
    //     assert |cnt| == |prev.cnt|;
    //     // //turn it into an array with the values needed and then turn it back into a sequence to assign
    //     // while ( i != |cnt|)
    //     // invariant 0 <= i <= |cnt|
    //     // decreases |cnt| - i
    //     // {
    //     //     //don't understand this error....
    //     //     // cnt[i] := prev.cnt[i] - tran.cnt_guard[i];
    //     //     // new;
    //     //     cnt := cnt[i := (prev.cnt[i] - tran.cnt_guard[i])];
    //     //     i := i + 1;
    //     // }
    // }
}

method pop_state(traces: seq<abstract_state>) returns (end_state: abstract_state, new_traces: seq<abstract_state>)
    requires |traces| > 0
    requires |traces| <= 100    //specific to the 8 reaction model (for shortest traces)
    //length of counters and species is > 0
    requires forall i :: 0 <= i < |traces| ==> |traces[i].cnt| > 0 && |traces[i].species| > 0
    //length of counters and species is same for all traces
    requires forall i :: 0 <= i < |traces| ==> forall j :: 0 <= j < |traces| ==> (|traces[i].cnt| == |traces[j].cnt| && |traces[i].species| == |traces[j].species|)
    //end state is the popped state
    ensures end_state == traces[|traces|-1]
    //returned traces is 1 shorter
    ensures |traces| - 1 == |new_traces|
    ensures |new_traces| < 100
    //end state counters and species need to be same length as those for everything else in new_traces
    ensures |end_state.cnt| > 0 && |end_state.species| > 0
    ensures forall i :: 0 <= i < |new_traces| ==> (|end_state.cnt| == |new_traces[i].cnt| && |end_state.species| == |new_traces[i].species|)
{
    end_state := traces[|traces|-1];
    new_traces := traces[..|traces|-1];
}

method push_state(new_state: abstract_state, traces: seq<abstract_state>) returns (new_traces: seq<abstract_state>)
    requires |new_state.cnt| > 0 && |new_state.species| > 0
    requires forall i :: 0 <= i < |traces| ==> |traces[i].cnt| > 0 && |traces[i].species| > 0
    ensures |new_traces| > 0
    ensures |traces| == |new_traces| - 1
    ensures new_traces[|new_traces|-1] == new_state
    ensures forall i :: 0 <= i < |new_traces| ==> |new_traces[i].cnt| > 0
    ensures forall i :: 0 <= i < |new_traces| ==> |new_traces[i].species| > 0
{
    new_traces := traces + [new_state];
}


//create a data type we can store the traces in probably a seq<seq> of states (which will need to have the counter information and previous reactions)

method successors(start_state: abstract_state, possible_trans: seq<transition>) returns(successors: seq<abstract_state>)
    requires 0 <= |start_state.prev_reactions|
    requires |start_state.cnt| > 0 && |start_state.species| > 0
    requires |possible_trans| >= 0
    requires forall i :: 0 <= i < |possible_trans| ==> |start_state.species| == |possible_trans[i].species_guard|
    requires forall i :: 0 <= i < |possible_trans| ==> |start_state.cnt| == |possible_trans[i].cnt_guard|
    //each transition starts with a length of at least 1
    ensures forall i :: 0 <= i < |successors| ==> |successors[i].prev_reactions| > 0
    //length of species vector is the same as for update vectors
    ensures forall i :: 0 <= i < |successors| ==> |start_state.species| == |successors[i].prev_reactions[|successors[i].prev_reactions|-1].cnt_guard|
    //ensures each new trace is one longer than input trace
    ensures forall i :: 0 <= i < |successors| ==> |successors[i].prev_reactions| + 1 == |start_state.prev_reactions|
    //ensure they are valid traces (check counters)
    ensures forall i :: 0 <= i < |successors| ==> forall j :: 0 <= j < |successors[i].prev_reactions[|successors[i].prev_reactions|-1].cnt_guard| ==> successors[i].prev_reactions[|successors[i].prev_reactions|-1].cnt_guard[j] >= start_state.species[j]
    ensures forall i :: 0 <= i < |successors| ==> |successors[i].cnt| > 0 && |successors[i].species| > 0
{
    successors := [];
    var i : int := 0;
    var j : int := 0;
    var k : int := 0;
    var add : bool := true;
    while(i != |possible_trans|)
    invariant 0 <= i <= |possible_trans|
    {
        j := 0;
        assert add;
        while (j != |possible_trans[i].cnt_guard|)
        invariant 0 <= j <= |possible_trans[i].cnt_guard|
        invariant add || exists k :: 0 <= k < |possible_trans[i].cnt_guard| && possible_trans[i].cnt_guard[k] > start_state.cnt[k]
        //we only are adding if sufficient cnt
        invariant add ==> (forall k :: 0 <= k < j ==> possible_trans[i].cnt_guard[k] <= start_state.cnt[k])
        {
            if (possible_trans[i].cnt_guard[j] > start_state.cnt[j]) 
            {
                add := false;
                assert possible_trans[i].cnt_guard[j] > start_state.cnt[j];
            }
            else{
                assert possible_trans[i].cnt_guard[j] <= start_state.cnt[j];
            }
            j := j + 1;
        }
        //why does this not hold
        assert add || exists k :: 0 <= k < |possible_trans[i].cnt_guard| && possible_trans[i].cnt_guard[k] > start_state.cnt[k];
        assert add ==> (forall k :: 0 <= k < |possible_trans[i].cnt_guard| ==> possible_trans[i].cnt_guard[k] <= start_state.cnt[k]);
        j := 0;
        while (j != |possible_trans[i].species_guard|)
        invariant 0 <= j <= |possible_trans[i].species_guard|
        invariant add || (exists k :: 0 <= k < |possible_trans[i].cnt_guard| && possible_trans[i].cnt_guard[k] > start_state.cnt[k]) || (exists l :: 0 <= l < |possible_trans[i].species_guard| && possible_trans[i].species_guard[l] > start_state.species[l])
        //only adding if sufficeint cnt and species
        invariant add ==> ((forall k :: 0 <= k < |possible_trans[i].cnt_guard| ==> possible_trans[i].cnt_guard[k] <= start_state.cnt[k]) && (forall l :: 0 <= l < j ==> possible_trans[i].species_guard[l] <= start_state.species[l]))
        {
            if (possible_trans[i].species_guard[j] > start_state.species[j]) 
            {
                add := false;
                assert possible_trans[i].species_guard[j] > start_state.species[j];
            }
            else{
                assert possible_trans[i].species_guard[j] <= start_state.species[j];
            }
            j := j + 1;
        }
        assert add || (exists k :: 0 <= k < |possible_trans[i].species_guard| && possible_trans[i].species_guard[k] > start_state.species[k]) || (exists n :: 0 <= n < |possible_trans[i].cnt_guard| && possible_trans[i].cnt_guard[n] > start_state.cnt[n]);
        //only adding if sufficeint cnt and species
        assert add ==> ((forall k :: 0 <= k < |possible_trans[i].cnt_guard| ==> possible_trans[i].cnt_guard[k] <= start_state.cnt[k]) && (forall l :: 0 <= l < j ==> possible_trans[i].species_guard[l] <= start_state.species[l]));
        if (add == true) 
        { 
            assert forall j :: 0 <= j < |possible_trans[i].species_guard| ==> possible_trans[i].species_guard[j] <= start_state.species[j];
            assert forall j :: 0 <= j < |possible_trans[i].cnt_guard| ==> possible_trans[i].cnt_guard[j] <= start_state.cnt[j];
            var tmp := new abstract_state.succ(start_state, possible_trans[i]);
        }
        add := true;
        i := i + 1;
    }
}
    

method walking_algorithm(init_state: ctrl_state, prefix_transitions: seq<transition>, possible_trans: seq<transition>, curr_species: seq<int>, curr_counters: seq<int>, ctrl_state_1_cnt: int, ctrl_state_2_cnt: int) returns(traces : seq<abstract_state>)
    requires ctrl_state_1_cnt + ctrl_state_2_cnt == 50 && ctrl_state_1_cnt >= 0 && ctrl_state_2_cnt >= 0 //specific to 8 reaction model
    //prefix has a length > 0
    requires |prefix_transitions| > 0
    //need some species and counters to keep track of
    requires |curr_species| > 0 && |curr_counters| > 0
    //all transitions have the same number of counters/species
    requires forall i :: 0 <= i < |possible_trans| ==> |possible_trans[i].cnt_guard| == |curr_counters|
    requires forall i :: 0 <= i < |possible_trans| ==> |possible_trans[i].species_guard| == |curr_species|
    //require all initial transition start in first ctrl abstract_state
    requires forall i :: 0 <= i < |prefix_transitions| ==> prefix_transitions[i].src == init_state
    //all values in counter are >= 0
    requires forall i :: 0 <= i < |curr_species| ==> curr_species[i] >= 0 
    requires forall i :: 0 <= i < |curr_counters| ==> curr_counters[i] >= 0 
    //***ensures each trace is unique      *****important*****
    ensures forall i :: 0 <= i < |traces| ==> forall j :: 0 <= j < |traces| ==> traces[i].prev_reactions != traces[j].prev_reactions
    //ensures each trace is length 100 (consider making this an input parameter)
    ensures forall i :: 0 <= i < |traces| ==> |traces[i].prev_reactions| == 100 //specific to 8 reaction model

    {
        //var init_vector := new species_vector([50, 0, 0, 49, 1, 50])
        var loop_traces : seq<abstract_state> := [];
        var start_state := new abstract_state.init(curr_counters, curr_species);
        assert |start_state.cnt| == |curr_counters|;
        assert |start_state.species| == |curr_species|;
        loop_traces := push_state(start_state, loop_traces);
        assert forall i :: 0 <= i < |loop_traces| ==> |loop_traces[i].cnt| > 0 && |loop_traces[i].species| > 0;
        assert forall i :: 0 <= i < |loop_traces| ==> |loop_traces[i].cnt| == |curr_counters| && |loop_traces[i].species| == |curr_species|;
        assert forall i :: 0 <= i < |possible_trans| ==> |possible_trans[i].cnt_guard| == |curr_counters|; 
        assert forall i :: 0 <= i < |possible_trans| ==> |possible_trans[i].species_guard| == |curr_species|;
        assert forall  i :: 0 <= i < |loop_traces| ==> forall j :: 0 <= j < |possible_trans| ==> (|loop_traces[i].cnt| == |possible_trans[j].cnt_guard| && |loop_traces[i].species| == |possible_trans[j].species_guard|);
        var temp_state;
        var temp_successors;
        var i : int;
        traces := [];
        while(|loop_traces| != 0)
        //*****add manual decreases clause
        invariant forall i :: 0 <= i < |loop_traces| ==> |loop_traces[i].cnt| > 0 && |loop_traces[i].species| > 0
        invariant forall i :: 0 <= i < |possible_trans| ==> |possible_trans[i].cnt_guard| == |curr_counters|
        invariant forall i :: 0 <= i < |possible_trans| ==> |possible_trans[i].species_guard| == |curr_species|
        invariant forall i :: 0 <= i < |loop_traces| ==> (|loop_traces[i].cnt| == |curr_counters| && |loop_traces[i].species| == |curr_species|)
        //invariant forall i :: 0 <= i < |possible_trans| ==> (|possible_trans[i].cnt_guard| == |curr_counters| && |possible_trans[i].species_guard| == |curr_species|)
        invariant |loop_traces| >= 0
        {
            temp_state, loop_traces := pop_state(loop_traces);
            assert forall i :: 0 <= i < |loop_traces| ==> (|loop_traces[i].cnt| == |temp_state.cnt| && |loop_traces[i].species| == |temp_state.species|);
            assert forall i :: 0 <= i < |loop_traces| ==> (|loop_traces[i].cnt| == |curr_counters| && |loop_traces[i].species| == |curr_species|);
            assert forall i :: 0 <= i < |loop_traces| ==> forall j :: 0 <= j < |possible_trans| ==> (|possible_trans[j].cnt_guard| == |loop_traces[i].cnt| && |possible_trans[j].species_guard| == |loop_traces[i].species|);
            assert |temp_state.cnt| == |curr_counters| && |temp_state.species| == |curr_species|;
            assert forall i :: 0 <= i < |possible_trans| ==> (|possible_trans[i].cnt_guard| == |temp_state.cnt| && |possible_trans[i].species_guard| == |temp_state.species|);
            temp_successors := successors(temp_state, possible_trans);
            if (|temp_successors| == 0) 
            {
                traces := traces + [temp_state];
                continue;
            }
            else
            {
                assert forall i :: 0 <= i < |temp_successors| ==> |temp_successors[i].cnt| > 0 && |temp_successors[i].species| > 0;
                i := 0;
                while (i != |temp_successors|)
                invariant forall i :: 0 <= i < |temp_successors| ==> |temp_successors[i].cnt| > 0 && |temp_successors[i].species| > 0
                invariant forall i :: 0 <= i < |loop_traces| ==> |loop_traces[i].cnt| > 0 && |loop_traces[i].species| > 0
                invariant 0 <= i <= |temp_successors|
                {
                    loop_traces := push_state(temp_successors[i], loop_traces);
                    i := i + 1;
                }
            }
        }
    }


//**** implement main method (right now it just feeds all the information to the walking algorithm but it will need to have preconditions that represent the information from the dependendency graph and post conditions about the result)
method main()
//write preconditions for inputs from dependency graph (will be used to determine things below)
//need species init values and counter values (from dependency graph)
//need the reactions associated with the edges
//need tier values for reactions
//need relation of reactions with each other if the same tier (AND/OR)
// ******everything implemented here is using information from the dependency graph
{
    var q0 := new ctrl_state(0, true);  //init ctrl abstract_state
    var q1 := new ctrl_state(1, false); // second ctrl abstract_state
    var species_vector := ([50, 0, 0]); //[R, RL, Gbg]
    var counter_vector := ([49, 1, 50]); //counters for [R3_1, R3_2, R5]
    var r3_1 := new transition(0, q0, q0, [-1, 0, 0], [-1, 1, 0]);
    var ctrl_state_tran := new transition(1, q0, q1, [0, 0, 0], [0, 0, 0]);
    var r3_2 := new transition(0, q1, q1, [0, -1, 0], [-1, 1, 0]);
    var r5 := new transition(0, q1, q1, [0, 0, -1], [0, -1, 1]);
    q0.add_trans(r3_1);
    q0.add_trans(ctrl_state_tran);
    q1.add_trans(r3_2);
    q1.add_trans(r5);
    //while()
    var init : seq<transition> := [r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1, r3_1];
    var i : int := 0;
    var traces : seq<abstract_state>;
    var q1_transitions : seq<transition> := [r3_2, r5];
    traces := walking_algorithm(q0, init, q1_transitions, species_vector, counter_vector, 49, 1);
    //consider looping through the walking algorithm for 49 downto 1 for the first number and from 1 to 49 for the second number
}