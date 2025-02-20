#!/usr/bin/env bash

#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SCRIPT_DIR=$(dirname ${BASH_SOURCE[0]})
echo "SCRIPT_DIR": $SCRIPT_DIR
export DL_PYTHON_HOME="$(cd ${SCRIPT_DIR}/../../src; pwd)"
export DL_PYTHON_DLLIB_HOME="$(cd ${SCRIPT_DIR}/../../src/../../dllib/src; pwd)"
export DL_PYTHON_ORCA_HOME="$(cd ${SCRIPT_DIR}/../../src/../../orca/src; pwd)"
export BIGDL_HOME="$(cd ${SCRIPT_DIR}/../../../..; pwd)"

echo "BIGDL_HOME: $BIGDL_HOME"
echo "SPARK_HOME": $SPARK_HOME
echo "DL_PYTHON_HOME": $DL_PYTHON_HOME

if [ -z ${SPARK_HOME+x} ]; then echo "SPARK_HOME is unset"; exit 1; else echo "SPARK_HOME is set to '$SPARK_HOME'"; fi

export PYSPARK_ZIP=`find $SPARK_HOME/python/lib  -type f -iname '*.zip' | tr "\n" ":"`

export PYTHONPATH=$PYTHONPATH:$DL_PYTHON_HOME:$DL_PYTHON_DLLIB_HOME:$PYSPARK_ZIP:$DL_PYTHON_ORCA_HOME:$PYSPARK_ZIP:$BIGDL_HOME/scala/dllib/src/main/resources/spark-bigdl.conf
echo "PYTHONPATH": $PYTHONPATH

export BIGDL_CLASSPATH=$(find $BIGDL_HOME/dist/lib/ -name "bigdl-orca*with-dependencies.jar" | head -n 1)
export BIGDL_CLASSPATH=$(find $BIGDL_HOME/dist/lib/ -name "bigdl-dllib*with-dependencies.jar" | head -n 1):$BIGDL_CLASSPATH
echo "BIGDL_CLASSPATH": $BIGDL_CLASSPATH

if [[ ($SPARK_HOME == *"2.2.0"*) || ($SPARK_HOME == *"2.1.1"*) || ($SPARK_HOME == *"1.6.4"*) ]]; then
    export PYTHON_EXECUTABLES=("python2.7" "python3.5" "python3.6")
else
    export PYTHON_EXECUTABLES=("python2.7" "python3.5")
fi

export TF_LIBS_PATH=$(python -c 'import site; print(site.getsitepackages()[0])')/bigdl/share/tflibs
echo "TF_LIBS_PATH: $TF_LIBS_PATH"

function run_notebook() {
    notebook_path=$1
    target_notebook_path=${DL_PYTHON_HOME}/tmp_${PYTHON_EXECUTABLE}.ipynb
    echo "Change kernel to $PYTHON_EXECUTABLE"
    sed "s/\"python.\"/\"$PYTHON_EXECUTABLE\"/g" $notebook_path > ${target_notebook_path}
    jupyter nbconvert --to notebook --execute \
      --ExecutePreprocessor.timeout=360 --output ${DL_PYTHON_HOME}/tmp_out.ipynb \
      $target_notebook_path
}

export -f run_notebook
