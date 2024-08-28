#!/bin/bash  

############################################################################
# Author: EPRGGGZ Gustavo Garcia G.                                        #
#                                                                          #
# Script to collect logfiles for Kubernetes Cluster based on Spider input  #
# The script wil also collect HELM charts configuration                    #
# To use, execute collect_ADP_logs.sh <namespace>                          #
#                                                                          #
############################################################################

############################################################################
#                               history
# 
# 2019-01-23   Keith Liu   fix bug when get logs of pod which may have more than one container
#                          add more resources for describe logs
#                          add timestamp in the log folder name and some improvement
#
############################################################################

#Fail if empty argument received
#if [[ "$#" != "1" ]]; then
#    echo "Wrong number of arguments"
#    echo "Usage collect_ADP_logs.sh <Kubernetes_namespace>"
#    echo "ex:"
#    echo "$0 default    #--- to gather the logs for namespace 'default'"
#    exit 1
#fi


namespace=$1
kubeconfig=$2
#Create a directory for placing the logs
log_base_dir=logs_${namespace}_$(date "+%Y-%m-%d-%H-%M-%S")
log_base_path=$PWD/${log_base_dir}
mkdir ${log_base_dir}

get_describe_info() {
    #echo "---------------------------------------"
    echo "-Getting logs for describe info-"
    #echo "---------------------------------------"
    #echo "---------------------------------------"

    des_dir=${log_base_path}/describe
    mkdir ${des_dir}
    for attr in statefulsets deployments services replicasets endpoints daemonsets persistentvolumeclaims configmap pods nodes events jobs persistentvolumes rolebindings roles secrets serviceaccounts storageclasses ingresses
        do 
            dir=`echo $attr | tr '[:lower:]' '[:upper:]'`
            mkdir ${des_dir}/$dir
            kubectl --namespace ${namespace} --kubeconfig ${kubeconfig} get $attr > ${des_dir}/$dir/$attr.txt
            echo "Getting describe information on $dir.."
            for i in `kubectl --namespace ${namespace} --kubeconfig ${kubeconfig} get $attr | grep -v NAME | awk '{print $1}'`
                do
                    kubectl --namespace ${namespace} --kubeconfig ${kubeconfig} describe  $attr  $i > ${des_dir}/$dir/$i.txt
                done
        done
}

get_pods_logs() {
    #echo "---------------------------------------"
    echo "-Getting logs per POD-"
    #echo "---------------------------------------"
    #echo "---------------------------------------"
    
    logs_dir=${log_base_path}/logs
    mkdir ${logs_dir}
    kubectl --namespace ${namespace} --kubeconfig ${kubeconfig} get pods > ${logs_dir}/kube_podstolog.txt
    for i in `kubectl --namespace ${namespace} --kubeconfig ${kubeconfig} get pods | grep -v NAME | awk '{print $1}'`
        do
            for j in `kubectl --namespace ${namespace} --kubeconfig ${kubeconfig} get pod $i -o jsonpath="{.spec['containers', 'initContainers'][*].name}"`
                do
                    kubectl --namespace ${namespace} --kubeconfig ${kubeconfig} logs $i -c $j > ${logs_dir}/${i}_${j}.txt 
                done
        done
}

get_helm_info() {
    #echo "-----------------------------------------"
    echo "-Getting Helm Charts for the deployments-"
    #echo "-----------------------------------------"
    #echo "-----------------------------------------"
    
    helm_dir=${log_base_path}/helm
    mkdir ${helm_dir}
    helm --namespace ${namespace} --kubeconfig ${kubeconfig} list > ${helm_dir}/helm_deployments.txt
    
    for i in `helm --namespace ${namespace} --kubeconfig ${kubeconfig} list| grep -v NAME | awk '{print $1}'`
        do 
            #echo $i
            helm get $i > ${helm_dir}/$i.txt
        done	
}

compress_files() {
    echo "Generating tar file and removing logs directory..."
    tar cvfz $PWD/${log_base_dir}.tgz ${log_base_dir}
    echo  -e "\e[1m\e[31mGenerated file $PWD/${log_base_dir}.tgz, Please collect and send to ADP Support!\e[0m"
    rm -r $PWD/${log_base_dir}
}

get_describe_info
get_pods_logs
get_helm_info
compress_files


