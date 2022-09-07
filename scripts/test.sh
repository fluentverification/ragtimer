make
# clear
/usr/bin/time -o prefix_time.txt python3 main.py 1000 each &> prefix_result.txt
# python3 main.py 505 each
rm test_v*