import {useEffect, useState} from 'react';
import {useToast} from '@/components/ui/use-toast';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import { formatTimestamp } from '@/lib/utils';
import { notifyError } from '@/components/ui/use-toast';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Button} from '@/components/ui/button';
import {Badge} from '@/components/ui/badge';
import {Progress} from '@/components/ui/progress';
import {Skeleton} from '@/components/ui/skeleton';
import {Activity, AlertTriangle, Cpu, Database, HardDrive, MemoryStick, Network, RefreshCw, Server} from 'lucide-react';
import {
    getGrepWiseMetrics,
    getHealthStatus,
    getHttpMetrics,
    getJvmMetrics,
    getSystemInfo,
    getSystemMetrics,
    HealthStatus,
    MetricValue,
    SystemInfo
} from '@/api/metrics';

// Helper function to format bytes to human-readable format
const formatBytes = (bytes: number, decimals = 2) => {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
};

// Helper function to format milliseconds to human-readable format
const formatMilliseconds = (ms: number) => {
    if (ms < 1000) return `${ms.toFixed(2)} ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(2)} s`;
    if (ms < 3600000) return `${(ms / 60000).toFixed(2)} min`;
    return `${(ms / 3600000).toFixed(2)} h`;
};

// Component for displaying a metric card
interface MetricCardProps {
    title: string;
    value: string | number;
    description?: string;
    icon?: React.ReactNode;
    status?: 'success' | 'warning' | 'error' | 'info';
    progress?: number;
    loading?: boolean;
}

const MetricCard = ({
                        title,
                        value,
                        description,
                        icon,
                        status = 'info',
                        progress,
                        loading = false
                    }: MetricCardProps) => {
    const statusColors = {
        success: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
        warning: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300',
        error: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300',
        info: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300'
    };

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium">
                    {title}
                </CardTitle>
                {icon && <div className="h-4 w-4 text-muted-foreground">{icon}</div>}
            </CardHeader>
            <CardContent>
                {loading ? (
                    <>
                        <Skeleton className="h-8 w-full mb-2"/>
                        {description && <Skeleton className="h-4 w-3/4"/>}
                    </>
                ) : (
                    <>
                        <div className="text-2xl font-bold">
                            {value}
                            {status !== 'info' && (
                                <Badge className={`ml-2 ${statusColors[status]}`}>
                                    {status.toUpperCase()}
                                </Badge>
                            )}
                        </div>
                        {progress !== undefined && (
                            <Progress value={progress} className="h-2 mt-2"/>
                        )}
                        {description && (
                            <CardDescription className="text-xs text-muted-foreground mt-1">
                                {description}
                            </CardDescription>
                        )}
                    </>
                )}
            </CardContent>
        </Card>
    );
};

// Main monitoring dashboard component
export default function MonitoringPage() {
    const {toast} = useToast();
    const [activeTab, setActiveTab] = useState('overview');
    const [refreshInterval, setRefreshInterval] = useState<number | null>(30000); // 30 seconds
    const [lastRefreshed, setLastRefreshed] = useState<Date>(new Date());

    // State for metrics data
    const [loading, setLoading] = useState(true);
    const [healthStatus, setHealthStatus] = useState<HealthStatus | null>(null);
    const [systemInfo, setSystemInfo] = useState<SystemInfo | null>(null);
    const [grepwiseMetrics, setGrepwiseMetrics] = useState<MetricValue[]>([]);
    const [jvmMetrics, setJvmMetrics] = useState<MetricValue[]>([]);
    const [systemMetrics, setSystemMetrics] = useState<MetricValue[]>([]);
    const [httpMetrics, setHttpMetrics] = useState<MetricValue[]>([]);

    // Function to fetch all metrics
    const fetchMetrics = async () => {
        setLoading(true);
        try {
            const [health, info, grepwise, jvm, system, http] = await Promise.all([
                getHealthStatus(),
                getSystemInfo(),
                getGrepWiseMetrics(),
                getJvmMetrics(),
                getSystemMetrics(),
                getHttpMetrics()
            ]);

            setHealthStatus(health);
            setSystemInfo(info);
            setGrepwiseMetrics(grepwise);
            setJvmMetrics(jvm);
            setSystemMetrics(system);
            setHttpMetrics(http);
            setLastRefreshed(new Date());
        } catch (error) {
            console.error('Error fetching metrics:', error);
            notifyError(error, 'Error', 'Failed to fetch metrics. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    // Set up automatic refresh
    useEffect(() => {
        fetchMetrics();

        let intervalId: NodeJS.Timeout | null = null;

        if (refreshInterval && process.env.NODE_ENV !== 'test') {
            intervalId = setInterval(fetchMetrics, refreshInterval);
        }

        return () => {
            if (intervalId) {
                clearInterval(intervalId);
            }
        };
    }, [refreshInterval]);

    // Helper function to find a metric by name
    const findMetric = (metrics: MetricValue[], name: string): MetricValue | undefined => {
        return metrics.find(metric => metric.name === name);
    };

    // Helper function to get a metric value
    const getMetricValue = (metrics: MetricValue[], name: string, defaultValue: number = 0): number => {
        const metric = findMetric(metrics, name);
        if (!metric || !metric.measurements || metric.measurements.length === 0) {
            return defaultValue;
        }
        return metric.measurements[0].value;
    };

    // Toggle refresh interval
    const toggleRefresh = () => {
        if (refreshInterval) {
            setRefreshInterval(null);
        } else {
            setRefreshInterval(30000);
            fetchMetrics();
        }
    };

    // Manual refresh
    const handleRefresh = () => {
        fetchMetrics();
    };

    // Get health status color
    const getHealthStatusColor = (status: string): 'success' | 'warning' | 'error' => {
        switch (status.toLowerCase()) {
            case 'up':
                return 'success';
            case 'down':
                return 'error';
            default:
                return 'warning';
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">System Monitoring</h1>
                    <p className="text-muted-foreground">
                        Monitor the health and performance of your GrepWise instance
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={toggleRefresh}
                    >
                        {refreshInterval ? 'Disable Auto-refresh' : 'Enable Auto-refresh'}
                    </Button>
                    <Button
                        variant="default"
                        size="sm"
                        onClick={handleRefresh}
                        disabled={loading}
                    >
                        <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`}/>
                        Refresh
                    </Button>
                </div>
            </div>

            <div className="text-sm text-muted-foreground">
                Last refreshed: {formatTimestamp(lastRefreshed.getTime())}
                {refreshInterval && (
                    <span> · Auto-refresh every {refreshInterval / 1000} seconds</span>
                )}
            </div>

            <Tabs defaultValue="overview" value={activeTab} onValueChange={setActiveTab}>
                <TabsList className="grid grid-cols-5 w-full md:w-auto">
                    <TabsTrigger value="overview">Overview</TabsTrigger>
                    <TabsTrigger value="grepwise">GrepWise</TabsTrigger>
                    <TabsTrigger value="jvm">JVM</TabsTrigger>
                    <TabsTrigger value="system">System</TabsTrigger>
                    <TabsTrigger value="http">HTTP</TabsTrigger>
                </TabsList>

                {/* Overview Tab */}
                <TabsContent value="overview" className="space-y-6">
                    {/* Health Status */}
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        <MetricCard
                            title="System Status"
                            value={healthStatus?.status || 'Unknown'}
                            description="Overall system health status"
                            icon={<Activity/>}
                            status={healthStatus ? getHealthStatusColor(healthStatus.status) : 'info'}
                            loading={loading}
                        />
                        <MetricCard
                            title="Application"
                            value={systemInfo?.app?.name || 'GrepWise'}
                            description={`Version: ${systemInfo?.app?.version || 'Unknown'}`}
                            icon={<Server/>}
                            loading={loading}
                        />
                        <MetricCard
                            title="Java Runtime"
                            value={systemInfo?.java?.runtime?.name || 'JVM'}
                            description={`Version: ${systemInfo?.java?.version || 'Unknown'}`}
                            icon={<Cpu/>}
                            loading={loading}
                        />
                        <MetricCard
                            title="Operating System"
                            value={systemInfo?.os?.name || 'Unknown'}
                            description={`${systemInfo?.os?.version || ''} (${systemInfo?.os?.arch || ''})`}
                            icon={<Server/>}
                            loading={loading}
                        />
                    </div>

                    {/* Key Metrics */}
                    <h2 className="text-xl font-semibold mt-6">Key Metrics</h2>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {/* CPU Usage */}
                        {systemMetrics.length > 0 && (
                            <MetricCard
                                title="CPU Usage"
                                value={`${(getMetricValue(systemMetrics, 'system.cpu.usage') * 100).toFixed(1)}%`}
                                description="System CPU usage"
                                icon={<Cpu/>}
                                progress={getMetricValue(systemMetrics, 'system.cpu.usage') * 100}
                                status={
                                    getMetricValue(systemMetrics, 'system.cpu.usage') > 0.8
                                        ? 'error'
                                        : getMetricValue(systemMetrics, 'system.cpu.usage') > 0.6
                                            ? 'warning'
                                            : 'success'
                                }
                                loading={loading}
                            />
                        )}

                        {/* Memory Usage */}
                        {jvmMetrics.length > 0 && (
                            <MetricCard
                                title="Memory Usage"
                                value={formatBytes(getMetricValue(jvmMetrics, 'jvm.memory.used'))}
                                description={`${formatBytes(getMetricValue(jvmMetrics, 'jvm.memory.used'))} / ${formatBytes(getMetricValue(jvmMetrics, 'jvm.memory.max'))}`}
                                icon={<MemoryStick/>}
                                progress={(getMetricValue(jvmMetrics, 'jvm.memory.used') / getMetricValue(jvmMetrics, 'jvm.memory.max')) * 100}
                                status={
                                    getMetricValue(jvmMetrics, 'jvm.memory.used') / getMetricValue(jvmMetrics, 'jvm.memory.max') > 0.8
                                        ? 'error'
                                        : getMetricValue(jvmMetrics, 'jvm.memory.used') / getMetricValue(jvmMetrics, 'jvm.memory.max') > 0.6
                                            ? 'warning'
                                            : 'success'
                                }
                                loading={loading}
                            />
                        )}

                        {/* Disk Space */}
                        {systemMetrics.length > 0 && (
                            <MetricCard
                                title="Disk Space"
                                value={formatBytes(getMetricValue(systemMetrics, 'disk.free'))}
                                description="Free disk space"
                                icon={<HardDrive/>}
                                status={
                                    getMetricValue(systemMetrics, 'disk.free') / getMetricValue(systemMetrics, 'disk.total') < 0.1
                                        ? 'error'
                                        : getMetricValue(systemMetrics, 'disk.free') / getMetricValue(systemMetrics, 'disk.total') < 0.2
                                            ? 'warning'
                                            : 'success'
                                }
                                loading={loading}
                            />
                        )}

                        {/* HTTP Requests */}
                        {httpMetrics.length > 0 && (
                            <MetricCard
                                title="HTTP Requests"
                                value={getMetricValue(httpMetrics, 'http.server.requests').toFixed(0)}
                                description="Total HTTP requests"
                                icon={<Network/>}
                                loading={loading}
                            />
                        )}
                    </div>

                    {/* Component Health */}
                    <h2 className="text-xl font-semibold mt-6">Component Health</h2>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {healthStatus && Object.entries(healthStatus.components).map(([key, component]) => (
                            <MetricCard
                                key={key}
                                title={key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1')}
                                value={component.status}
                                icon={
                                    key === 'db' ? <Database/> :
                                        key === 'diskSpace' ? <HardDrive/> :
                                            <Activity/>
                                }
                                status={getHealthStatusColor(component.status)}
                                loading={loading}
                            />
                        ))}
                    </div>
                </TabsContent>

                {/* GrepWise Tab */}
                <TabsContent value="grepwise" className="space-y-6">
                    <h2 className="text-xl font-semibold">GrepWise Metrics</h2>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {grepwiseMetrics.map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('grepwise.', '').replace(/\./g, ' ').replace(/([A-Z])/g, ' $1')}
                                value={
                                    metric.baseUnit === 'bytes'
                                        ? formatBytes(metric.measurements[0].value)
                                        : metric.baseUnit === 'milliseconds'
                                            ? formatMilliseconds(metric.measurements[0].value)
                                            : metric.measurements[0].value.toString()
                                }
                                description={metric.description}
                                loading={loading}
                            />
                        ))}
                        {grepwiseMetrics.length === 0 && !loading && (
                            <div className="col-span-full flex items-center justify-center p-6 border rounded-lg">
                                <div className="text-center">
                                    <AlertTriangle className="h-8 w-8 text-yellow-500 mx-auto mb-2"/>
                                    <h3 className="text-lg font-medium">No GrepWise metrics found</h3>
                                    <p className="text-sm text-muted-foreground">
                                        Custom GrepWise metrics are not available or have not been configured.
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>
                </TabsContent>

                {/* JVM Tab */}
                <TabsContent value="jvm" className="space-y-6">
                    <h2 className="text-xl font-semibold">JVM Metrics</h2>

                    {/* Memory */}
                    <h3 className="text-lg font-medium mt-4">Memory</h3>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {jvmMetrics.filter(m => m.name.startsWith('jvm.memory')).map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('jvm.memory.', '').replace(/\./g, ' ')}
                                value={
                                    metric.baseUnit === 'bytes'
                                        ? formatBytes(metric.measurements[0].value)
                                        : metric.measurements[0].value.toString()
                                }
                                description={metric.description}
                                loading={loading}
                            />
                        ))}
                    </div>

                    {/* Garbage Collection */}
                    <h3 className="text-lg font-medium mt-6">Garbage Collection</h3>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {jvmMetrics.filter(m => m.name.startsWith('jvm.gc')).map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('jvm.gc.', '').replace(/\./g, ' ')}
                                value={
                                    metric.baseUnit === 'milliseconds'
                                        ? formatMilliseconds(metric.measurements[0].value)
                                        : metric.baseUnit === 'bytes'
                                            ? formatBytes(metric.measurements[0].value)
                                            : metric.measurements[0].value.toString()
                                }
                                description={metric.description}
                                loading={loading}
                            />
                        ))}
                    </div>

                    {/* Threads */}
                    <h3 className="text-lg font-medium mt-6">Threads</h3>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {jvmMetrics.filter(m => m.name.startsWith('jvm.threads')).map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('jvm.threads.', '').replace(/\./g, ' ')}
                                value={metric.measurements[0].value.toString()}
                                description={metric.description}
                                loading={loading}
                            />
                        ))}
                    </div>
                </TabsContent>

                {/* System Tab */}
                <TabsContent value="system" className="space-y-6">
                    <h2 className="text-xl font-semibold">System Metrics</h2>

                    {/* CPU */}
                    <h3 className="text-lg font-medium mt-4">CPU</h3>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {systemMetrics.filter(m => m.name.includes('cpu')).map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('system.', '').replace('process.', '').replace(/\./g, ' ')}
                                value={
                                    metric.name.includes('usage')
                                        ? `${(metric.measurements[0].value * 100).toFixed(1)}%`
                                        : metric.measurements[0].value.toString()
                                }
                                description={metric.description}
                                progress={
                                    metric.name.includes('usage')
                                        ? metric.measurements[0].value * 100
                                        : undefined
                                }
                                loading={loading}
                            />
                        ))}
                    </div>

                    {/* Disk */}
                    <h3 className="text-lg font-medium mt-6">Disk</h3>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {systemMetrics.filter(m => m.name.startsWith('disk')).map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('disk.', '').replace(/\./g, ' ')}
                                value={
                                    metric.baseUnit === 'bytes'
                                        ? formatBytes(metric.measurements[0].value)
                                        : metric.measurements[0].value.toString()
                                }
                                description={metric.description}
                                loading={loading}
                            />
                        ))}
                    </div>

                    {/* Process */}
                    <h3 className="text-lg font-medium mt-6">Process</h3>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        {systemMetrics.filter(m => m.name.startsWith('process') && !m.name.includes('cpu')).map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('process.', '').replace(/\./g, ' ')}
                                value={
                                    metric.baseUnit === 'bytes'
                                        ? formatBytes(metric.measurements[0].value)
                                        : metric.measurements[0].value.toString()
                                }
                                description={metric.description}
                                loading={loading}
                            />
                        ))}
                    </div>
                </TabsContent>

                {/* HTTP Tab */}
                <TabsContent value="http" className="space-y-6">
                    <h2 className="text-xl font-semibold">HTTP Metrics</h2>
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {httpMetrics.map(metric => (
                            <MetricCard
                                key={metric.name}
                                title={metric.name.replace('http.server.', '').replace('tomcat.', '').replace(/\./g, ' ')}
                                value={
                                    metric.baseUnit === 'milliseconds'
                                        ? formatMilliseconds(metric.measurements[0].value)
                                        : metric.measurements[0].value.toString()
                                }
                                description={metric.description}
                                loading={loading}
                            />
                        ))}
                        {httpMetrics.length === 0 && !loading && (
                            <div className="col-span-full flex items-center justify-center p-6 border rounded-lg">
                                <div className="text-center">
                                    <AlertTriangle className="h-8 w-8 text-yellow-500 mx-auto mb-2"/>
                                    <h3 className="text-lg font-medium">No HTTP metrics found</h3>
                                    <p className="text-sm text-muted-foreground">
                                        HTTP metrics are not available or have not been configured.
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>
                </TabsContent>
            </Tabs>
        </div>
    );
}